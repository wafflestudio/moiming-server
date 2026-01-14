package com.wafflestudio.spring2025.domain.registration.service

import com.wafflestudio.spring2025.domain.event.EventDeadlinePassedException
import com.wafflestudio.spring2025.domain.event.EventFullException
import com.wafflestudio.spring2025.domain.event.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.RegistrationAlreadyCanceledException
import com.wafflestudio.spring2025.domain.registration.RegistrationAlreadyExistsException
import com.wafflestudio.spring2025.domain.registration.RegistrationInvalidTokenException
import com.wafflestudio.spring2025.domain.registration.RegistrationNotFoundException
import com.wafflestudio.spring2025.domain.registration.RegistrationWrongEmailException
import com.wafflestudio.spring2025.domain.registration.RegistrationWrongNameException
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationDto
import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationWithEventDto
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.model.RegistrationToken
import com.wafflestudio.spring2025.domain.registration.model.RegistrationTokenPurpose
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationTokenRepository
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
        val deadline = event.registrationDeadline
        if (deadline != null && Instant.now().isAfter(deadline)) {
            throw EventDeadlinePassedException()
        }
        val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val currentConfirmed =
            registrationRepository
                .countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
                .toInt()

        if (userId != null && registrationRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw RegistrationAlreadyExistsException()
        }

        val status =
            if (capacity > currentConfirmed) {
                RegistrationStatus.CONFIRMED
            } else if (event.waitlistEnabled) {
                RegistrationStatus.WAITING
            } else {
                throw EventFullException()
            }

        val registration =
            if (userId == null) {
                if (guestName == null || guestName.length <= 1) {
                    throw RegistrationWrongNameException()
                }
                if (guestEmail.isNullOrBlank() || !emailRegex.matches(guestEmail)) {
                    throw RegistrationWrongEmailException()
                }
                if (registrationRepository.existsByGuestEmailAndEventId(guestEmail, eventId)) {
                    throw RegistrationAlreadyExistsException()
                }
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

        val saved = registrationRepository.save(registration)
        val cancelToken = generateToken()
        registrationTokenRepository.save(
            RegistrationToken(
                registrationId = saved.id ?: throw RegistrationNotFoundException(),
                tokenHash = hashToken(cancelToken),
                purpose = RegistrationTokenPurpose.CANCEL,
            ),
        )
        return CreateRegistrationResponse(RegistrationDto(saved), cancelToken)
    }

    fun getByEventId(eventId: Long): List<RegistrationDto> =
        registrationRepository
            .findByEventId(eventId)
            .map { registration -> RegistrationDto(registration) }

    fun getByUserId(userId: Long): List<RegistrationDto> =
        registrationRepository
            .findByUserId(userId)
            .map { registration -> RegistrationDto(registration) }

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

    fun update(): Unit = throw UnsupportedOperationException("참여 신청 내 투표 수정은 나중에 구현")

    fun confirm(registrationId: Long) {
        val registration =
            registrationRepository
                .findById(registrationId)
                .orElseThrow { RegistrationNotFoundException() }
        if (registration.status == RegistrationStatus.CANCELED) {
            throw RegistrationAlreadyCanceledException()
        }
        if (registration.status == RegistrationStatus.CONFIRMED) {
            return
        }

        val event = eventRepository.findById(registration.eventId).orElseThrow { EventNotFoundException() }
        val capacity = event.capacity ?: throw IllegalStateException("이벤트의 capacity가 설정되어 있지 않습니다.")
        val confirmed =
            registrationRepository.countByEventIdAndStatus(registration.eventId, RegistrationStatus.CONFIRMED)
        if (capacity <= confirmed.toInt()) {
            throw EventFullException()
        }

        registration.status = RegistrationStatus.CONFIRMED
        registrationRepository.save(registration)
    }

    fun cancelWithToken(
        registrationId: Long,
        token: String,
    ) {
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

        val registration =
            registrationRepository
                .findById(registrationId)
                .orElseThrow { RegistrationNotFoundException() }
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

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(token.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun generateToken(): String = UUID.randomUUID().toString().replace("-", "")

    @Transactional
    fun reconcileWaitlist(eventId: Long) {
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

        waitings.take(available).forEach { it.status = RegistrationStatus.CONFIRMED }
        registrationRepository.saveAll(waitings.take(available))
    }
}
