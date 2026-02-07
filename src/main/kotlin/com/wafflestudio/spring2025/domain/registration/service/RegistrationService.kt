package com.wafflestudio.spring2025.domain.registration.service

import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.event.exception.EventFullException
import com.wafflestudio.spring2025.domain.event.exception.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventLockRepository
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.dto.response.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.EventRegistrationItem
import com.wafflestudio.spring2025.domain.registration.dto.response.GetEventRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.GetMyRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.GetRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.MyRegistrationItem
import com.wafflestudio.spring2025.domain.registration.dto.response.PatchRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.response.RegistrationStatusResponse
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationConflictException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationErrorCode
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationForbiddenException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationNotFoundException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationValidationException
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.registration.service.command.CreateRegistrationCommand
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository,
    private val eventLockRepository: EventLockRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val tokenValidity = Duration.ofHours(24)

    @Transactional
    fun create(command: CreateRegistrationCommand): CreateRegistrationResponse =
        when (command) {
            is CreateRegistrationCommand.Member ->
                createInternal(
                    userId = command.userId,
                    eventId = command.eventId,
                    guestName = null,
                    guestEmail = null,
                )

            is CreateRegistrationCommand.Guest ->
                createInternal(
                    userId = null,
                    eventId = command.eventId,
                    guestName = command.name,
                    guestEmail = command.email,
                )
        }

    private fun createInternal(
        userId: Long?,
        eventId: String,
        guestName: String?,
        guestEmail: String?,
    ): CreateRegistrationResponse {
        val event = eventRepository.findByPublicId(eventId) ?: throw EventNotFoundException()
        val eventPk = event.id ?: throw EventNotFoundException()

        eventLockRepository.lockById(eventPk)
        val lockedEvent: Event = eventRepository.findById(eventPk).orElseThrow { EventNotFoundException() }

        if (!isRegistrationEnabled(lockedEvent)) {
            throw RegistrationValidationException(RegistrationErrorCode.NOT_WITHIN_REGISTRATION_WINDOW)
        }

        val capacity = lockedEvent.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val currentConfirmed =
            registrationRepository
                .countByEventIdAndStatus(eventPk, RegistrationStatus.CONFIRMED)
                .toInt()

        val status =
            if (capacity > currentConfirmed) {
                RegistrationStatus.CONFIRMED
            } else if (lockedEvent.waitlistEnabled) {
                RegistrationStatus.WAITLISTED
            } else {
                throw EventFullException()
            }

        val existingRegistration =
            if (userId == null) {
                if (guestName == null || guestName.length <= 1) {
                    throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_WRONG_NAME)
                }
                if (guestEmail.isNullOrBlank() || !emailRegex.matches(guestEmail)) {
                    throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_WRONG_EMAIL)
                }
                registrationRepository.findByGuestEmailAndEventId(guestEmail, eventPk)
            } else {
                registrationRepository.findByUserIdAndEventId(userId, eventPk)
            }

        val saved =
            try {
                if (existingRegistration != null) {
                    when (existingRegistration.status) {
                        RegistrationStatus.BANNED ->
                            throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)

                        RegistrationStatus.CANCELED -> {
                            existingRegistration.status = status
                            if (userId == null) {
                                existingRegistration.guestName = guestName
                                existingRegistration.guestEmail = guestEmail
                            }
                            registrationRepository.save(existingRegistration)
                        }

                        RegistrationStatus.CONFIRMED,
                        RegistrationStatus.HOST,
                        RegistrationStatus.WAITLISTED,
                        -> throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_EXISTS)
                    }
                } else {
                    val registration =
                        if (userId == null) {
                            Registration(
                                userId = null,
                                eventId = eventPk,
                                guestName = guestName,
                                guestEmail = guestEmail,
                                status = status,
                            )
                        } else {
                            Registration(
                                userId = userId,
                                eventId = eventPk,
                                guestName = null,
                                guestEmail = null,
                                status = status,
                            )
                        }
                    registrationRepository.save(registration)
                }
            } catch (e: DuplicateKeyException) {
                // ✅ 유니크 인덱스(이벤트+유저 / 이벤트+게스트이메일)로 막힌 경우
                throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_EXISTS)
            }

        val waitlistedNumber: Int? =
            if (saved.status == RegistrationStatus.WAITLISTED) {
                registrationRepository
                    .countByEventIdAndStatus(eventPk, RegistrationStatus.WAITLISTED)
                    .toInt()
            } else {
                null
            }

        val user =
            userId?.let { id ->
                userRepository.findById(id).orElse(null)
            }

        val recipientEmail = user?.email ?: guestEmail

        if (!recipientEmail.isNullOrBlank()) {
            val confirmedCount =
                registrationRepository
                    .countByEventIdAndStatus(eventPk, RegistrationStatus.CONFIRMED)
                    .toInt()

            val emailData =
                EmailService.RegistrationStatusEmailData(
                    toEmail = recipientEmail,
                    status = saved.status,
                    name = saved.guestName ?: user?.name ?: "참여자",
                    eventTitle = lockedEvent.title,
                    startsAt = lockedEvent.startsAt,
                    endsAt = lockedEvent.endsAt,
                    location = lockedEvent.location,
                    confirmedCount = confirmedCount,
                    capacity = lockedEvent.capacity,
                    registrationStartsAt = lockedEvent.registrationStartsAt,
                    registrationEndsAt = lockedEvent.registrationEndsAt,
                    description = lockedEvent.description,
                    publicId = lockedEvent.publicId,
                    registrationPublicId = saved.registrationPublicId,
                    waitingNum = waitlistedNumber,
                )

            afterCommit {
                emailService.sendRegistrationStatusEmail(emailData)
            }
        }

        return CreateRegistrationResponse(
            registrationPublicId = saved.registrationPublicId,
        )
    }

    private fun toResponseStatus(status: RegistrationStatus): RegistrationStatusResponse =
        when (status) {
            RegistrationStatus.HOST -> RegistrationStatusResponse.CONFIRMED
            RegistrationStatus.CONFIRMED -> RegistrationStatusResponse.CONFIRMED
            RegistrationStatus.WAITLISTED -> RegistrationStatusResponse.WAITLISTED
            RegistrationStatus.CANCELED -> RegistrationStatusResponse.CANCELED
            RegistrationStatus.BANNED -> RegistrationStatusResponse.BANNED
        }

    fun getMyRegistrations(
        userId: Long,
        page: Int,
        size: Int,
    ): GetMyRegistrationsResponse {
        val pageable = PageRequest.of(page, size)
        val paged = registrationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        if (paged.isEmpty()) {
            return GetMyRegistrationsResponse(registrations = emptyList())
        }

        val eventIds = paged.map { it.eventId }.distinct()
        val eventsById =
            eventRepository.findAllById(eventIds).associateBy { event ->
                event.id ?: throw EventNotFoundException()
            }

        val countsByEventId =
            registrationRepository
                .countByEventIdsAndStatuses(
                    eventIds = eventIds,
                    statuses = listOf(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED),
                ).associate { it.eventId to it.totalCount.toInt() }

        val waitlistedByRegistrationId =
            registrationRepository
                .findWaitlistPositionsByEventIds(eventIds, RegistrationStatus.WAITLISTED)
                .associate { it.registrationPublicId to it.waitlistNumber.toInt() }

        val items =
            paged.map { registration ->
                val event = eventsById[registration.eventId] ?: throw EventNotFoundException()
                val waitlistedNumber: Int? =
                    if (registration.status == RegistrationStatus.WAITLISTED) {
                        waitlistedByRegistrationId[registration.registrationPublicId]
                    } else {
                        null
                    }

                MyRegistrationItem(
                    registration = registration,
                    event = event,
                    registrationCnt = countsByEventId[registration.eventId] ?: 0,
                    waitlistedNum = waitlistedNumber,
                )
            }

        return GetMyRegistrationsResponse(registrations = items)
    }

    @Transactional
    fun updateStatus(
        userId: Long?,
        registrationId: String,
        status: RegistrationStatus,
    ): PatchRegistrationResponse {
        val registrationPreview =
            registrationRepository.findByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()
        val eventPk = registrationPreview.eventId

        eventLockRepository.lockById(eventPk)
        val event = eventRepository.findById(eventPk).orElseThrow { EventNotFoundException() }

        val registration =
            registrationRepository.lockByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()

        if (!isRegistrationEnabled(event)) {
            throw RegistrationValidationException(RegistrationErrorCode.NOT_WITHIN_REGISTRATION_WINDOW)
        }

        val isHost = userId != null && userId == event.createdBy
        val isRegistrant = userId != null && registration.userId == userId

        if (status == RegistrationStatus.BANNED && !isHost) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_PATCH_UNAUTHORIZED)
        }
        if (status == RegistrationStatus.CANCELED && !isHost && !isRegistrant && userId != null) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_PATCH_UNAUTHORIZED)
        }

        val wasConfirmed = registration.status == RegistrationStatus.CONFIRMED
        when (status) {
            RegistrationStatus.BANNED -> {
                if (registration.status == RegistrationStatus.BANNED) {
                    throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_BANNED)
                }
                registration.status = RegistrationStatus.BANNED
                val saved = registrationRepository.save(registration)

                sendStatusEmailAfterUpdate(
                    registration = saved,
                    event = event,
                )
            }

            RegistrationStatus.CANCELED -> {
                if (registration.status == RegistrationStatus.BANNED) {
                    throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)
                }
                if (registration.status == RegistrationStatus.CANCELED) {
                    throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_CANCELED)
                }
                registration.status = RegistrationStatus.CANCELED
                val saved = registrationRepository.save(registration)

                sendStatusEmailAfterUpdate(
                    registration = saved,
                    event = event,
                )
            }

            RegistrationStatus.CONFIRMED,
            RegistrationStatus.HOST,
            RegistrationStatus.WAITLISTED,
            -> throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)
        }

        if (wasConfirmed) {
            reconcileWaitlist(eventPk)
        }

        val patchEmail =
            registration.userId?.let { id ->
                userRepository.findById(id).orElse(null)?.email
            } ?: registration.guestEmail

        return PatchRegistrationResponse(patchEmail = patchEmail)
    }

    fun getEventRegistration(
        eventId: String,
        requesterId: Long?,
        status: String?,
        orderBy: String?,
        cursor: Int?,
    ): GetEventRegistrationsResponse {
        val event = eventRepository.findByPublicId(eventId) ?: throw EventNotFoundException()
        val eventPk = event.id ?: throw EventNotFoundException()
        val isAdmin = requesterId != null && event.createdBy == requesterId

        val statusFilter = parseStatusFilter(status)
        val order = parseOrderBy(orderBy)

        val pageSize = 10
        val offset = (cursor ?: 0).coerceAtLeast(0)

        val totalCount =
            statusFilter?.let {
                registrationRepository.countByEventIdAndStatus(eventPk, it).toInt()
            } ?: registrationRepository.countByEventId(eventPk).toInt()

        val registrationsPage =
            when (order) {
                RegistrationOrderBy.NAME ->
                    statusFilter?.let {
                        registrationRepository.findPageByEventIdAndStatusOrderByNameAsc(
                            eventId = eventPk,
                            status = it,
                            limit = pageSize,
                            offset = offset,
                        )
                    } ?: registrationRepository.findPageByEventIdOrderByNameAsc(
                        eventId = eventPk,
                        limit = pageSize,
                        offset = offset,
                    )

                RegistrationOrderBy.REGISTERED_AT ->
                    statusFilter?.let {
                        registrationRepository.findPageByEventIdAndStatusOrderByCreatedAtAsc(
                            eventId = eventPk,
                            status = it,
                            limit = pageSize,
                            offset = offset,
                        )
                    } ?: registrationRepository.findPageByEventIdOrderByCreatedAtAsc(
                        eventId = eventPk,
                        limit = pageSize,
                        offset = offset,
                    )
            }

        val userIds = registrationsPage.mapNotNull { it.userId }.distinct()
        val usersById =
            if (userIds.isEmpty()) {
                emptyMap()
            } else {
                userRepository.findAllById(userIds).associateBy { it.id!! }
            }

        val waitlistedPublicIds =
            registrationsPage
                .filter { it.status == RegistrationStatus.WAITLISTED }
                .map { it.registrationPublicId }

        val waitlistedByPublicId =
            if (waitlistedPublicIds.isEmpty()) {
                emptyMap()
            } else {
                registrationRepository
                    .findWaitlistPositionsByRegistrationPublicIds(
                        eventId = eventPk,
                        status = RegistrationStatus.WAITLISTED,
                        registrationPublicIds = waitlistedPublicIds,
                    ).associate { it.registrationPublicId to it.waitlistNumber.toInt() }
            }

        val page =
            registrationsPage.map { registration ->
                val user = registration.userId?.let { usersById[it] }
                val waitlistedNumber =
                    if (registration.status == RegistrationStatus.WAITLISTED) {
                        waitlistedByPublicId[registration.registrationPublicId]
                    } else {
                        null
                    }

                val profileImage = user?.profileImage
                val item =
                    EventRegistrationItem(
                        registration = registration,
                        profileImage = profileImage,
                        user = user,
                        waitingNum = waitlistedNumber,
                    )
                if (isAdmin) item else item.copy(email = null)
            }

        val hasNext = (offset + page.size) < totalCount
        val nextCursor = if (hasNext && page.isNotEmpty()) offset + page.size else null

        return GetEventRegistrationsResponse(
            participants = page,
            totalCount = totalCount,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private enum class RegistrationOrderBy {
        NAME,
        REGISTERED_AT,
    }

    private fun parseStatusFilter(status: String?): RegistrationStatus? =
        when (status?.lowercase()) {
            null -> null
            "confirmed" -> RegistrationStatus.CONFIRMED
            // "waiting" 문자열은 혹시 남아있는 클라이언트 호환 위해 두고 싶으면 유지 가능
            "waiting", "waitlisted" -> RegistrationStatus.WAITLISTED
            "canceled" -> RegistrationStatus.CANCELED
            "banned" -> RegistrationStatus.BANNED
            else -> throw RegistrationValidationException(RegistrationErrorCode.INVALID_REGISTRATION_QUERY_PARAMETER)
        }

    private fun parseOrderBy(orderBy: String?): RegistrationOrderBy =
        when (orderBy?.lowercase()) {
            null, "registeredat" -> RegistrationOrderBy.REGISTERED_AT
            "name" -> RegistrationOrderBy.NAME
            else -> RegistrationOrderBy.REGISTERED_AT
        }

    fun getRegistrationInformation(
        registrationId: String,
        user: User?,
    ): GetRegistrationResponse {
        val registration =
            registrationRepository.findByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()

        if (user != null &&
            user.email != registration.guestEmail
        ) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_VIEW_UNAUTHORIZED)
        }

        val status = registration.status
        val waitlistPosition =
            if (status == RegistrationStatus.WAITLISTED) {
                val waitlistedRegs =
                    registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                        registration.eventId,
                        RegistrationStatus.WAITLISTED,
                    )
                val idx = waitlistedRegs.indexOfFirst { it.id == registration.id }
                if (idx >= 0) idx + 1 else 0
            } else {
                0
            }

        val registrationUser =
            registration.userId?.let { uid ->
                userRepository.findById(uid).orElse(null)
            }

        val reservationEmail = registrationUser?.email ?: registration.guestEmail
        val guestName = registrationUser?.name ?: registration.guestName

        return GetRegistrationResponse(
            status = status,
            guestName = guestName.orEmpty(),
            waitlistPosition = waitlistPosition,
            registrationPublicId = registration.registrationPublicId,
            reservationEmail = reservationEmail.orEmpty(),
        )
    }

    private fun isRegistrationEnabled(eventId: Long): Boolean {
        val event =
            eventRepository
                .findById(eventId)
                .orElseThrow { EventNotFoundException() }
        return isRegistrationEnabled(event)
    }

    private fun isRegistrationEnabled(event: Event): Boolean {
        val registrationStartsAt = event.registrationStartsAt
        val registrationEndsAt = event.registrationEndsAt
        val now = Instant.now()

        // null 정책: 시작/끝이 null이면 제한 없는 것으로 취급
        val afterStart = registrationStartsAt?.let { !now.isBefore(it) } ?: true
        val beforeEnd = registrationEndsAt?.let { !now.isAfter(it) } ?: true

        return afterStart && beforeEnd
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(token.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun generateToken(): String = UUID.randomUUID().toString().replace("-", "")

    private fun sendStatusEmailAfterUpdate(
        registration: Registration,
        event: Event,
    ) {
        val registrationUser =
            registration.userId?.let { uid ->
                userRepository.findById(uid).orElse(null)
            }

        val recipientEmail = registrationUser?.email ?: registration.guestEmail
        if (recipientEmail.isNullOrBlank()) return

        val confirmedCount =
            registrationRepository
                .countByEventIdAndStatus(event.id!!, RegistrationStatus.CONFIRMED)
                .toInt()

        val emailData =
            EmailService.RegistrationStatusEmailData(
                toEmail = recipientEmail,
                status = registration.status,
                name = registrationUser?.name ?: registration.guestName ?: "참여자",
                eventTitle = event.title,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                location = event.location,
                confirmedCount = confirmedCount,
                capacity = event.capacity,
                registrationStartsAt = event.registrationStartsAt,
                registrationEndsAt = event.registrationEndsAt,
                description = event.description,
                publicId = event.publicId,
                registrationPublicId = registration.registrationPublicId,
            )

        afterCommit {
            emailService.sendRegistrationStatusEmail(emailData)
        }
    }

    @Transactional
    fun reconcileWaitlist(eventId: Long) {
        eventLockRepository.lockById(eventId)

        val event: Event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        if (!isRegistrationEnabled(event)) return

        val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val confirmed =
            registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED).toInt()
        val available = capacity - confirmed
        if (available <= 0) return

        val waitlistedRegs =
            registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventId,
                RegistrationStatus.WAITLISTED,
            )

        val waitlistNumbers =
            waitlistedRegs.withIndex().associate { indexed ->
                indexed.value.registrationPublicId to (indexed.index + 1)
            }

        val promoted = waitlistedRegs.take(available)
        promoted.forEach { it.status = RegistrationStatus.CONFIRMED }
        registrationRepository.saveAll(promoted)

        val confirmedAfter = confirmed + promoted.size

        val emailDataList =
            promoted.mapNotNull { registration ->
                val user: User? =
                    registration.userId?.let { uid ->
                        userRepository.findById(uid).orElse(null)
                    }

                val recipientEmail = user?.email ?: registration.guestEmail
                val recipientName = user?.name ?: registration.guestName ?: "참여자"
                val waitingNum = waitlistNumbers[registration.registrationPublicId]

                if (recipientEmail.isNullOrBlank()) {
                    null
                } else {
                    WaitlistPromotionEmailData(
                        toEmail = recipientEmail,
                        eventTitle = event.title,
                        name = recipientName,
                        waitingNum = waitingNum,
                        startsAt = event.startsAt,
                        endsAt = event.endsAt,
                        location = event.location,
                        confirmedCount = confirmedAfter,
                        capacity = capacity,
                        registrationStartsAt = event.registrationStartsAt,
                        registrationEndsAt = event.registrationEndsAt,
                        description = event.description,
                        eventPublicId = event.publicId,
                        registrationPublicId = registration.registrationPublicId,
                    )
                }
            }

        afterCommit {
            emailDataList.forEach { data ->
                emailService.sendWaitlistPromotionEmail(
                    toEmail = data.toEmail,
                    eventTitle = data.eventTitle,
                    name = data.name,
                    waitingNum = data.waitingNum,
                    startsAt = data.startsAt,
                    endsAt = data.endsAt,
                    location = data.location,
                    confirmedCount = data.confirmedCount,
                    capacity = data.capacity,
                    registrationStartsAt = data.registrationStartsAt,
                    registrationEndsAt = data.registrationEndsAt,
                    description = data.description,
                    eventPublicId = data.eventPublicId,
                    registrationPublicId = data.registrationPublicId,
                )
            }
        }
    }

    private data class WaitlistPromotionEmailData(
        val toEmail: String,
        val eventTitle: String,
        val name: String,
        val waitingNum: Int?,
        val startsAt: Instant?,
        val endsAt: Instant?,
        val location: String?,
        val confirmedCount: Int?,
        val capacity: Int?,
        val registrationStartsAt: Instant?,
        val registrationEndsAt: Instant?,
        val description: String?,
        val eventPublicId: String,
        val registrationPublicId: String,
    )

    private fun afterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            },
        )
    }
}
