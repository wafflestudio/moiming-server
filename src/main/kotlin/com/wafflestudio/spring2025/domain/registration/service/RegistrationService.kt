package com.wafflestudio.spring2025.domain.registration.service

import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.event.exception.EventFullException
import com.wafflestudio.spring2025.domain.event.exception.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.EventRegistrationItem
import com.wafflestudio.spring2025.domain.registration.dto.GetEventRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.dto.GetMyRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.dto.GetRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.MyRegistrationItem
import com.wafflestudio.spring2025.domain.registration.dto.PatchRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationGuestsResponse
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationGuestsResponse.Guest
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationStatusResponse
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationConflictException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationErrorCode
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationForbiddenException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationNotFoundException
import com.wafflestudio.spring2025.domain.registration.exception.RegistrationValidationException
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.model.RegistrationToken
import com.wafflestudio.spring2025.domain.registration.model.RegistrationTokenPurpose
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationTokenRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository,
    private val registrationTokenRepository: RegistrationTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val tokenValidity = Duration.ofHours(24)

    fun create(
        userId: Long?,
        eventId: String,
        guestName: String?,
        guestEmail: String?,
    ): CreateRegistrationResponse {
        val event = eventRepository.findByPublicId(eventId) ?: throw EventNotFoundException()
        val eventPk = event.id ?: throw EventNotFoundException()

        if (!isRegistrationEnabled(eventPk)) {
            throw RegistrationValidationException(RegistrationErrorCode.NOT_WITHIN_REGISTRATION_WINDOW)
        }

        val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val currentConfirmed =
            registrationRepository
                .countByEventIdAndStatus(eventPk, RegistrationStatus.CONFIRMED)
                .toInt()

        val status =
            if (capacity > currentConfirmed) {
                RegistrationStatus.CONFIRMED
            } else if (event.waitlistEnabled) {
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
            if (existingRegistration != null) {
                when (existingRegistration.status) {
                    RegistrationStatus.BANNED -> throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)

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

        val cancelToken = generateToken()
        registrationTokenRepository.save(
            RegistrationToken(
                registrationId = saved.id ?: throw RegistrationNotFoundException(),
                tokenHash = hashToken(cancelToken),
                purpose = RegistrationTokenPurpose.CANCEL,
            ),
        )

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

            emailService.sendRegistrationStatusEmail(
                EmailService.RegistrationStatusEmailData(
                    toEmail = recipientEmail,
                    status = saved.status,
                    name = saved.guestName ?: user?.name ?: "참여자",
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
                    registrationPublicId = saved.registrationPublicId,
                    waitingNum = waitlistedNumber,
                ),
            )
        }

        return CreateRegistrationResponse(
            status = toResponseStatus(saved.status),
            waitingNum = waitlistedNumber,
            confirmEmail = recipientEmail,
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

    fun getGuestsByEventId(
        eventId: String,
        requesterId: Long,
    ): RegistrationGuestsResponse {
        val event = eventRepository.findByPublicId(eventId) ?: throw EventNotFoundException()
        val eventPk = event.id ?: throw EventNotFoundException()
        if (event.createdBy != requesterId) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_UNAUTHORIZED)
        }

        val guests =
            registrationRepository
                .findByEventId(eventPk)
                .filter { it.status == RegistrationStatus.CONFIRMED }
                .map { registration ->
                    val user = registration.userId?.let { userId -> userRepository.findById(userId).orElse(null) }
                    Guest(
                        registrationPublicId = registration.registrationPublicId,
                        name = user?.name ?: registration.guestName.orEmpty(),
                        email = user?.email ?: registration.guestEmail,
                        profileImage = null,
                    )
                }

        val confirmedCount =
            registrationRepository
                .countByEventIdAndStatus(eventPk, RegistrationStatus.CONFIRMED)
                .toInt()

        val waitlistedCount =
            registrationRepository
                .countByEventIdAndStatus(eventPk, RegistrationStatus.WAITLISTED)
                .toInt()

        return RegistrationGuestsResponse(
            guests = guests,
            confirmedCount = confirmedCount,
            waitingCount = waitlistedCount, // 응답 필드명이 waitingCount면 유지 (내부 변수만 waitlisted로)
        )
    }

    fun getMyRegistrations(
        userId: Long,
        page: Int,
        size: Int,
    ): GetMyRegistrationsResponse {
        val registrations = registrationRepository.findByUserIdOrderByCreatedAtDesc(userId)
        val paged = registrations.drop(page * size).take(size)
        if (paged.isEmpty()) {
            return GetMyRegistrationsResponse(registrations = emptyList())
        }

        val eventIds = paged.map { it.eventId }.distinct()
        val eventsById =
            eventRepository.findAllById(eventIds).associateBy { event ->
                event.id ?: throw EventNotFoundException()
            }

        val countsByEventId =
            eventIds.associateWith { eventId ->
                val confirmed =
                    registrationRepository
                        .countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
                        .toInt()
                val waitlisted =
                    registrationRepository
                        .countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)
                        .toInt()
                confirmed + waitlisted
            }

        val waitlistedRegsByEventId =
            eventIds.associateWith { eventId ->
                registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(eventId, RegistrationStatus.WAITLISTED)
            }

        val items =
            paged.map { registration ->
                val event = eventsById[registration.eventId] ?: throw EventNotFoundException()
                val waitlistedNumber: Int? =
                    if (registration.status == RegistrationStatus.WAITLISTED) {
                        val waitlistedList = waitlistedRegsByEventId[registration.eventId].orEmpty()
                        val index = waitlistedList.indexOfFirst { it.id == registration.id }
                        if (index >= 0) index + 1 else null
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

    fun updateStatus(
        userId: Long,
        registrationId: String,
        status: RegistrationStatus,
    ): PatchRegistrationResponse {
        val registration =
            registrationRepository.findByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()
        val eventPk = registration.eventId

        if (!isRegistrationEnabled(eventPk)) {
            throw RegistrationValidationException(RegistrationErrorCode.NOT_WITHIN_REGISTRATION_WINDOW)
        }

        val event = eventRepository.findById(eventPk).orElseThrow { EventNotFoundException() }

        val isHost = userId == event.createdBy
        val isRegistrant = registration.userId == userId

        if (status == RegistrationStatus.BANNED && !isHost) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_UNAUTHORIZED)
        }
        if (status == RegistrationStatus.CANCELED && !isHost && !isRegistrant) {
            throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_UNAUTHORIZED)
        }

        val wasConfirmed = registration.status == RegistrationStatus.CONFIRMED
        when (status) {
            RegistrationStatus.BANNED -> {
                if (registration.status == RegistrationStatus.BANNED) {
                    throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_BANNED)
                }
                registration.status = RegistrationStatus.BANNED
                registrationRepository.save(registration)
            }

            RegistrationStatus.CANCELED -> {
                if (registration.status == RegistrationStatus.BANNED) {
                    throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)
                }
                if (registration.status == RegistrationStatus.CANCELED) {
                    throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_CANCELED)
                }
                registration.status = RegistrationStatus.CANCELED
                registrationRepository.save(registration)
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
        val isAdmin = event.createdBy == requesterId

        val statusFilter = parseStatusFilter(status)
        val order = parseOrderBy(orderBy)

        val registrations = registrationRepository.findByEventId(eventPk)
        val filtered =
            statusFilter?.let { filter ->
                registrations.filter { it.status == filter }
            } ?: registrations

        val waitlistedRegs =
            registrations
                .filter { it.status == RegistrationStatus.WAITLISTED }
                .sortedBy { it.createdAt }

        val waitlistedIndexById =
            waitlistedRegs
                .mapIndexedNotNull { idx, reg -> reg.id?.let { id -> id to (idx + 1) } }
                .toMap()

        val userIds = filtered.mapNotNull { it.userId }.distinct()
        val usersById =
            if (userIds.isEmpty()) {
                emptyMap()
            } else {
                userRepository.findAllById(userIds).associateBy { it.id!! }
            }

        val items =
            filtered.map { registration ->
                val user = registration.userId?.let { usersById[it] }
                val waitlistedNumber =
                    if (registration.status == RegistrationStatus.WAITLISTED) {
                        registration.id?.let { waitlistedIndexById[it] }
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

        val sorted =
            when (order) {
                RegistrationOrderBy.NAME ->
                    items.sortedWith(compareBy<EventRegistrationItem> { it.name.lowercase() }.thenBy { it.registrationId })

                RegistrationOrderBy.REGISTERED_AT ->
                    items.sortedWith(compareBy<EventRegistrationItem> { it.createdAt }.thenBy { it.registrationId })
            }

        val pageSize = 10
        val safeCursor = cursor ?: -1
        val startIndex = (safeCursor + 1).coerceAtLeast(0)
        val page =
            if (startIndex >= sorted.size) {
                emptyList()
            } else {
                sorted.drop(startIndex).take(pageSize)
            }

        val lastIndex = startIndex + page.size - 1
        val hasNext = (startIndex + page.size) < sorted.size
        val nextCursor = if (hasNext && page.isNotEmpty()) lastIndex else null
        val totalCount = items.size

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
            else -> throw RegistrationValidationException(RegistrationErrorCode.REGISTRATION_INVALID_STATUS)
        }

    private fun parseOrderBy(orderBy: String?): RegistrationOrderBy =
        when (orderBy?.lowercase()) {
            null, "registeredat" -> RegistrationOrderBy.REGISTERED_AT
            "name" -> RegistrationOrderBy.NAME
            else -> RegistrationOrderBy.REGISTERED_AT
        }

    fun cancelWithToken(
        registrationId: String,
        token: String,
    ) {
        val registration =
            registrationRepository.findByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()

        val registrationPk = registration.id ?: throw RegistrationNotFoundException()

        if (!isRegistrationEnabled(registration.eventId)) {
            throw RegistrationValidationException(RegistrationErrorCode.NOT_WITHIN_REGISTRATION_WINDOW)
        }

        val tokenHash = hashToken(token)
        val registrationToken =
            registrationTokenRepository.findByTokenHashAndPurpose(
                tokenHash,
                RegistrationTokenPurpose.CANCEL,
            ) ?: throw RegistrationForbiddenException(
                RegistrationErrorCode.REGISTRATION_INVALID_TOKEN,
            )

        val tokenCreatedAt =
            registrationToken.createdAt ?: throw RegistrationForbiddenException(
                RegistrationErrorCode.REGISTRATION_INVALID_TOKEN,
            )

        if (tokenCreatedAt.plus(tokenValidity).isBefore(Instant.now())) {
            registrationTokenRepository.delete(registrationToken)
            throw RegistrationForbiddenException(
                RegistrationErrorCode.REGISTRATION_INVALID_TOKEN,
            )
        }

        if (registrationToken.registrationId != registrationPk) {
            throw RegistrationForbiddenException(
                RegistrationErrorCode.REGISTRATION_INVALID_TOKEN,
            )
        }

        if (registration.status == RegistrationStatus.CANCELED) {
            throw RegistrationConflictException(RegistrationErrorCode.REGISTRATION_ALREADY_CANCELED)
        }

        val wasConfirmed = registration.status == RegistrationStatus.CONFIRMED
        registration.status = RegistrationStatus.CANCELED
        registrationRepository.save(registration)

        if (wasConfirmed) {
            reconcileWaitlist(registration.eventId)
        }
        registrationTokenRepository.delete(registrationToken)
    }

    fun getRegistrationInformation(
        registrationId: String,
        userId: Long,
    ): GetRegistrationResponse {
        val registration =
            registrationRepository.findByRegistrationPublicId(registrationId)
                ?: throw RegistrationNotFoundException()

        if (userId != registration.userId) throw RegistrationForbiddenException(RegistrationErrorCode.REGISTRATION_UNAUTHORIZED)

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

        val reservationEmail =
            registration.userId?.let { uid ->
                userRepository.findById(uid).orElse(null)?.email
            } ?: registration.guestEmail

        return GetRegistrationResponse(
            status = status,
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

    @Transactional
    fun reconcileWaitlist(eventId: Long) {
        if (!isRegistrationEnabled(eventId)) {
            return
        }
        val event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val confirmed = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
        val available = capacity - confirmed.toInt()

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

        val confirmedAfter = confirmed.toInt() + promoted.size

        promoted.forEach { registration ->
            val user =
                registration.userId?.let { uid ->
                    userRepository.findById(uid).orElse(null)
                }

            val recipientEmail = user?.email ?: registration.guestEmail
            val recipientName = user?.name ?: registration.guestName ?: "참여자"
            val waitingNum = waitlistNumbers[registration.registrationPublicId]

            if (!recipientEmail.isNullOrBlank()) {
                emailService.sendWaitlistPromotionEmail(
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
    }
}
