package com.wafflestudio.spring2025.domain.registration.service

import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.event.exception.EventErrorCode
import com.wafflestudio.spring2025.domain.event.exception.EventFullException
import com.wafflestudio.spring2025.domain.event.exception.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.exception.EventValidationException
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.RegistrationAlreadyBannedException
import com.wafflestudio.spring2025.domain.registration.RegistrationAlreadyCanceledException
import com.wafflestudio.spring2025.domain.registration.RegistrationAlreadyExistsException
import com.wafflestudio.spring2025.domain.registration.RegistrationInvalidStatusException
import com.wafflestudio.spring2025.domain.registration.RegistrationInvalidTokenException
import com.wafflestudio.spring2025.domain.registration.RegistrationNotFoundException
import com.wafflestudio.spring2025.domain.registration.RegistrationUnauthorizedException
import com.wafflestudio.spring2025.domain.registration.RegistrationWrongEmailException
import com.wafflestudio.spring2025.domain.registration.RegistrationWrongNameException
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.EventRegistrationItem
import com.wafflestudio.spring2025.domain.registration.dto.GetEventRegistrationsResponse
import com.wafflestudio.spring2025.domain.registration.dto.PatchRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationGuestsResponse
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationGuestsResponse.Guest
import com.wafflestudio.spring2025.domain.registration.dto.RegistrationStatusResponse
import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationDto
import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationWithEventDto
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
        eventId: Long,
        guestName: String?,
        guestEmail: String?,
    ): CreateRegistrationResponse {
        val event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        if (isRegistrationEnabled(eventId)) {
            val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
            val currentConfirmed =
                registrationRepository
                    .countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
                    .toInt()

            val status =
                if (capacity > currentConfirmed) {
                    RegistrationStatus.CONFIRMED
                } else if (event.waitlistEnabled) {
                    RegistrationStatus.WAITING
                } else {
                    throw EventFullException()
                }

            val existingRegistration =
                if (userId == null) {
                    if (guestName == null || guestName.length <= 1) {
                        throw RegistrationWrongNameException()
                    }
                    if (guestEmail.isNullOrBlank() || !emailRegex.matches(guestEmail)) {
                        throw RegistrationWrongEmailException()
                    }
                    registrationRepository.findByGuestEmailAndEventId(guestEmail, eventId)
                } else {
                    registrationRepository.findByUserIdAndEventId(userId, eventId)
                }

            val saved =
                if (existingRegistration != null) {
                    when (existingRegistration.status) {
                        RegistrationStatus.BANNED -> throw RegistrationInvalidStatusException()
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
                        RegistrationStatus.WAITING,
                        -> throw RegistrationAlreadyExistsException()
                    }
                } else {
                    val registration =
                        if (userId == null) {
                            Registration(
                                userId = null,
                                eventId = eventId,
                                guestName = guestName,
                                guestEmail = guestEmail,
                                status = status,
                            )
                        } else {
                            Registration(
                                userId = userId,
                                eventId = eventId,
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
            val waitingNum =
                if (saved.status == RegistrationStatus.WAITING) {
                    registrationRepository
                        .countByEventIdAndStatus(eventId, RegistrationStatus.WAITING)
                        .toInt()
                } else {
                    null
                }

            val recipientEmail =
                if (userId != null) {
                    userRepository.findById(userId).orElse(null)?.email
                } else {
                    guestEmail
                }
            if (!recipientEmail.isNullOrBlank()) {
                emailService.sendRegistrationStatusEmail(
                    toEmail = recipientEmail,
                    eventTitle = event.title,
                    status = saved.status,
                    waitingNum = waitingNum,
                )
            }
            return CreateRegistrationResponse(
                status = toResponseStatus(saved.status),
                waitingNum = waitingNum,
                confirmEmail = recipientEmail,
            )
        } else {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }
    }

    private fun toResponseStatus(status: RegistrationStatus): RegistrationStatusResponse =
        when (status) {
            RegistrationStatus.HOST -> RegistrationStatusResponse.CONFIRMED
            RegistrationStatus.CONFIRMED -> RegistrationStatusResponse.CONFIRMED
            RegistrationStatus.WAITING -> RegistrationStatusResponse.WAITING
            RegistrationStatus.CANCELED -> RegistrationStatusResponse.CANCELLED
            RegistrationStatus.BANNED -> RegistrationStatusResponse.BANNED
        }

    fun getGuestsByEventId(
        eventId: Long,
        requesterId: Long,
    ): RegistrationGuestsResponse {
        val event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        if (event.createdBy != requesterId) {
            throw RegistrationUnauthorizedException()
        }

        val guests =
            registrationRepository
                .findByEventId(eventId)
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
                .countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
                .toInt()
        val waitingCount =
            registrationRepository
                .countByEventIdAndStatus(eventId, RegistrationStatus.WAITING)
                .toInt()

        return RegistrationGuestsResponse(
            guests = guests,
            confirmedCount = confirmedCount,
            waitingCount = waitingCount,
        )
    }

    fun getByUserId(userId: Long): List<RegistrationDto> =
        registrationRepository
            .findByUserId(userId)
            .map { registration -> RegistrationDto(registration) }

    fun getByPublicId(
        eventId: Long,
        registrationPublicId: String,
        requesterId: Long,
    ): RegistrationDto {
        val event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        if (event.createdBy != requesterId) {
            throw RegistrationUnauthorizedException()
        }
        val registration =
            registrationRepository.findByRegistrationPublicId(registrationPublicId)
                ?: throw RegistrationNotFoundException()
        if (registration.eventId != eventId) {
            throw RegistrationNotFoundException()
        }
        return RegistrationDto(registration)
    }

    fun getByUserIdWithEvents(
        userId: Long,
        page: Int,
        size: Int,
    ): List<RegistrationWithEventDto> {
        val registrations = registrationRepository.findByUserIdOrderByCreatedAtDesc(userId)
        val paged = registrations.drop(page * size).take(size)
        if (paged.isEmpty()) {
            return emptyList()
        }

        val eventIds = paged.map { it.eventId }.distinct()
        val eventsById =
            eventRepository.findAllById(eventIds).associateBy { event ->
                event.id ?: throw EventNotFoundException()
            }

        return paged.map { registration ->
            val event = eventsById[registration.eventId] ?: throw EventNotFoundException()
            RegistrationWithEventDto(registration, event)
        }
    }

    fun updateStatus(
        userId: Long,
        registrationId: Long,
        status: RegistrationStatus,
    ): PatchRegistrationResponse {
        val registration =
            registrationRepository
                .findById(registrationId)
                .orElseThrow { RegistrationNotFoundException() }
        val eventId = registration.eventId

        if ((isRegistrationEnabled(eventId))) {
            val event =
                eventRepository
                    .findById(registration.eventId)
                    .orElseThrow { EventNotFoundException() }

            val isHost = userId == event.createdBy
            val isRegistrant = registration.userId == userId

            if (status == RegistrationStatus.BANNED && !isHost) {
                throw RegistrationUnauthorizedException()
            }
            if (status == RegistrationStatus.CANCELED && !isHost && !isRegistrant) {
                throw RegistrationUnauthorizedException()
            }

            val wasConfirmed = registration.status == RegistrationStatus.CONFIRMED
            when (status) {
                RegistrationStatus.BANNED -> {
                    if (registration.status == RegistrationStatus.BANNED) {
                        throw RegistrationAlreadyBannedException()
                    }
                    registration.status = RegistrationStatus.BANNED
                    registrationRepository.save(registration)
                }

                RegistrationStatus.CANCELED -> {
                    if (registration.status == RegistrationStatus.BANNED) {
                        throw RegistrationInvalidStatusException()
                    }
                    if (registration.status == RegistrationStatus.CANCELED) {
                        throw RegistrationAlreadyCanceledException()
                    }
                    registration.status = RegistrationStatus.CANCELED
                    registrationRepository.save(registration)
                }

                RegistrationStatus.CONFIRMED,
                RegistrationStatus.HOST,
                RegistrationStatus.WAITING,
                -> throw RegistrationInvalidStatusException()
            }

            if (wasConfirmed) {
                reconcileWaitlist(registration.eventId)
            }

            val patchEmail =
                registration.userId?.let { id ->
                    userRepository.findById(id).orElse(null)?.email
                } ?: registration.guestEmail

            return PatchRegistrationResponse(patchEmail = patchEmail)
        } else {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }
    }

    fun getEventRegistration(
        eventId: Long,
        requesterId: Long,
        status: String?,
        orderBy: String?,
        cursor: Int?,
    ): GetEventRegistrationsResponse {
        val event = eventRepository.findById(eventId).orElseThrow { EventNotFoundException() }
        val isAdmin = event.createdBy == requesterId

        val statusFilter = parseStatusFilter(status)
        val order = parseOrderBy(orderBy)

        val registrations = registrationRepository.findByEventId(eventId)
        val filtered =
            statusFilter?.let { filter ->
                registrations.filter { it.status == filter }
            } ?: registrations

        val waitings =
            registrations
                .filter { it.status == RegistrationStatus.WAITING }
                .sortedBy { it.createdAt }
        val waitingIndexById =
            waitings
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
                val waitingNum =
                    if (registration.status == RegistrationStatus.WAITING) {
                        registration.id?.let { waitingIndexById[it] }
                    } else {
                        null
                    }
                val profileImage = user?.profileImage
                val item =
                    EventRegistrationItem(
                        registration = registration,
                        profileImage = profileImage,
                        user = user,
                        waitingNum = waitingNum,
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

        return GetEventRegistrationsResponse(
            participants = page,
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
            "waiting" -> RegistrationStatus.WAITING
            "canceled" -> RegistrationStatus.CANCELED
            "banned" -> RegistrationStatus.BANNED
            else -> throw RegistrationInvalidStatusException()
        }

    private fun parseOrderBy(orderBy: String?): RegistrationOrderBy =
        when (orderBy?.lowercase()) {
            null, "registeredat" -> RegistrationOrderBy.REGISTERED_AT
            "name" -> RegistrationOrderBy.NAME
            else -> RegistrationOrderBy.REGISTERED_AT
        }

    fun cancelWithToken(
        registrationId: Long,
        token: String,
    ) {
        val registration =
            registrationRepository
                .findById(registrationId)
                .orElseThrow { RegistrationNotFoundException() }
        if (!isRegistrationEnabled(registration.eventId)) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }
        val tokenHash = hashToken(token)
        val registrationToken =
            registrationTokenRepository.findByTokenHashAndPurpose(tokenHash, RegistrationTokenPurpose.CANCEL)
                ?: throw RegistrationInvalidTokenException()
        val tokenCreatedAt = registrationToken.createdAt ?: throw RegistrationInvalidTokenException()
        if (tokenCreatedAt.plus(tokenValidity).isBefore(Instant.now())) {
            registrationTokenRepository.delete(registrationToken)
            throw RegistrationInvalidTokenException()
        }
        if (registrationToken.registrationId != registrationId) {
            throw RegistrationInvalidTokenException()
        }

        if (registration.status == RegistrationStatus.CANCELED) {
            throw RegistrationAlreadyCanceledException()
        }
        val wasConfirmed = registration.status == RegistrationStatus.CONFIRMED
        registration.status = RegistrationStatus.CANCELED
        registrationRepository.save(registration)
        if (wasConfirmed) {
            reconcileWaitlist(registration.eventId)
        }
        registrationTokenRepository.delete(registrationToken)
    }

    private fun isRegistrationEnabled(eventId: Long): Boolean {
        val event =
            eventRepository
                .findById(eventId)
                .orElseThrow { EventNotFoundException() }

        val registrationStartsAt = event.registrationStart
        val registrationEndsAt = event.registrationDeadline
        val now = Instant.now()

        if (now.isBefore(registrationEndsAt) and now.isAfter(registrationStartsAt)) {
            return true
        } else {
            return false
        }
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

        val waitings =
            registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventId,
                RegistrationStatus.WAITING,
            )

        val promoted = waitings.take(available)
        promoted.forEach { it.status = RegistrationStatus.CONFIRMED }
        registrationRepository.saveAll(promoted)

        promoted.forEach { registration ->
            val recipientEmail =
                registration.userId?.let { userId ->
                    userRepository.findById(userId).orElse(null)?.email
                } ?: registration.guestEmail
            if (!recipientEmail.isNullOrBlank()) {
                emailService.sendWaitlistPromotionEmail(
                    toEmail = recipientEmail,
                    eventTitle = event.title,
                )
            }
        }
    }
}
