package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.domain.event.dto.response.EventDetailResponse
import com.wafflestudio.spring2025.domain.event.dto.response.GuestPreview
import com.wafflestudio.spring2025.domain.event.dto.response.MyRole
import com.wafflestudio.spring2025.domain.event.exception.EventErrorCode
import com.wafflestudio.spring2025.domain.event.exception.EventForbiddenException
import com.wafflestudio.spring2025.domain.event.exception.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.exception.EventValidationException
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 일정 생성
     * - API 설계상 body를 비우고 201 + Location을 주기 위해 생성된 publicId를 반환
     */
    fun create(
        title: String,
        description: String?,
        location: String?,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean,
        registrationStart: Instant?,
        registrationDeadline: Instant?,
        createdBy: Long,
    ): String {
        validateCreateOrUpdate(
            title = title,
            startAt = startAt,
            endAt = endAt,
            capacity = capacity,
            registrationStart = registrationStart,
            registrationDeadline = registrationDeadline,
        )

        val event =
            Event(
                publicId = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description,
                location = location,
                startAt = startAt,
                endAt = endAt,
                capacity = capacity,
                waitlistEnabled = waitlistEnabled,
                registrationStart = registrationStart,
                registrationDeadline = registrationDeadline,
                createdBy = createdBy,
            )

        val saved = eventRepository.save(event)
        return saved.publicId
    }

    /**
     * 일정 상세 조회
     * - registrations 도메인 붙기 전: participants/waiting/guests는 기본값으로 내려줌
     */
    fun getDetail(
        publicId: String,
        requesterId: Long,
    ): EventDetailResponse {
        val event = getEventByPublicId(publicId)

        val eventId =
            requireNotNull(event.id) {
                // publicId로 조회된 이벤트는 정상이라면 id가 있어야 함 (저장 후 조회 상태)
                "Event internal id is null: publicId=$publicId"
            }

        val isCreator = event.createdBy == requesterId

        // 내 신청 정보(있으면)
        val myRegistration =
            registrationRepository.findByUserIdAndEventId(
                userId = requesterId,
                eventId = eventId,
            )

        val myRole =
            when {
                isCreator -> MyRole.CREATOR
                myRegistration != null && myRegistration.status != RegistrationStatus.CANCELED -> MyRole.PARTICIPANT
                else -> MyRole.NONE
            }

        val currentParticipants =
            registrationRepository
                .countByEventIdAndStatus(
                    eventID = eventId,
                    registrationStatus = RegistrationStatus.CONFIRMED,
                ).toInt()

        val waitingNum: Int? =
            if (myRegistration?.status == RegistrationStatus.WAITLISTED) {
                val waitings =
                    registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                        eventID = eventId,
                        registrationStatus = RegistrationStatus.WAITLISTED,
                    )
                val idx = waitings.indexOfFirst { it.id == myRegistration.id }
                if (idx >= 0) idx + 1 else null
            } else {
                null
            }

        // ✅ 참여자 미리보기: CONFIRMED 중 userId가 있는 애들만(예: 최대 5명)
        val confirmedRegs =
            registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventID = eventId,
                registrationStatus = RegistrationStatus.CONFIRMED,
            )

        val userIds = confirmedRegs.mapNotNull { it.userId }.distinct().take(5)

        val usersById =
            userRepository
                .findAllById(userIds)
                .associateBy { it.id!! }

        val guestsPreview =
            userIds.mapNotNull { uid ->
                val u = usersById[uid] ?: return@mapNotNull null
                GuestPreview(
                    id = u.id!!,
                    name = u.name,
                    profileImage = u.profileImage,
                )
            }

        return EventDetailResponse(
            title = event.title,
            description = event.description,
            location = event.location,
            startAt = event.startAt,
            endAt = event.endAt,
            capacity = event.capacity,
            currentParticipants = currentParticipants,
            registrationStart = event.registrationStart,
            registrationDeadline = event.registrationDeadline,
            myRole = myRole,
            waitingNum = waitingNum,
            guestsPreview = guestsPreview,
        )
    }

    fun update(
        publicId: String,
        title: String?,
        description: String?,
        location: String?,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean?,
        registrationStart: Instant?,
        registrationDeadline: Instant?,
        requesterId: Long,
    ): Event {
        val event = getEventByPublicId(publicId)

        // 생성자 권한 체크
        requireCreator(event, requesterId)

        // null은 "변경 없음"
        title?.let {
            if (it.isBlank()) {
                throw EventValidationException(EventErrorCode.EVENT_TITLE_BLANK)
            }
            event.title = it.trim()
        }
        description?.let { event.description = it }
        location?.let { event.location = it }
        startAt?.let { event.startAt = it }
        endAt?.let { event.endAt = it }
        capacity?.let { event.capacity = it }
        waitlistEnabled?.let { event.waitlistEnabled = it }
        registrationStart?.let { event.registrationStart = it }
        registrationDeadline?.let { event.registrationDeadline = it }

        // 변경 후에도 도메인 규칙 검증
        validateCreateOrUpdate(
            title = event.title,
            startAt = event.startAt,
            endAt = event.endAt,
            capacity = event.capacity,
            registrationStart = event.registrationStart,
            registrationDeadline = event.registrationDeadline,
        )

        return eventRepository.save(event)
    }

    fun delete(
        publicId: String,
        requesterId: Long,
    ) {
        val event = getEventByPublicId(publicId)

        // 생성자 권한 체크
        requireCreator(event, requesterId)

        // 내부 PK로 삭제
        eventRepository.deleteById(requireNotNull(event.id))
    }

    private fun getEventByPublicId(publicId: String): Event =
        eventRepository.findByPublicId(publicId)
            ?: throw EventNotFoundException(publicId)

    private fun requireCreator(
        event: Event,
        requesterId: Long,
    ) {
        if (event.createdBy != requesterId) {
            throw EventForbiddenException(requesterId)
        }
    }

    private fun validateCreateOrUpdate(
        title: String,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        registrationStart: Instant?,
        registrationDeadline: Instant?,
    ) {
        if (title.isBlank()) {
            throw EventValidationException(EventErrorCode.EVENT_TITLE_BLANK)
        }

        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw EventValidationException(EventErrorCode.EVENT_TIME_RANGE_INVALID)
        }

        if (capacity != null && capacity <= 0) {
            throw EventValidationException(EventErrorCode.EVENT_CAPACITY_INVALID)
        }

        // registrationStart / registrationDeadline 관계
        if (registrationStart != null &&
            registrationDeadline != null &&
            registrationStart.isAfter(registrationDeadline)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }

        // registrationDeadline <= startAt
        if (registrationDeadline != null &&
            startAt != null &&
            registrationDeadline.isAfter(startAt)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }

        // registrationStart <= startAt
        if (registrationStart != null &&
            startAt != null &&
            registrationStart.isAfter(startAt)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }
    }
}
