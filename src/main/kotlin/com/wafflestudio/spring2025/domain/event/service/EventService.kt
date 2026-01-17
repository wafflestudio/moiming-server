package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.dto.response.EventDetailResponse
import com.wafflestudio.spring2025.domain.event.dto.response.GuestPreview
import com.wafflestudio.spring2025.domain.event.dto.response.MyRole
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import org.springframework.stereotype.Service
import java.time.Instant
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val userRepository: UserRepository,
) {

    /**
     * 일정 생성
     * - API 설계상 body를 비우고 201 + Location을 주기 위해 생성된 eventId를 반환
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
    ): Long {
        validateCreateOrUpdate(
            title = title,
            startAt = startAt,
            endAt = endAt,
            capacity = capacity,
            registrationStart = registrationStart,
            registrationDeadline = registrationDeadline,
        )

        val event = Event(
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
        return requireNotNull(saved.id) { "Saved event id is null" }
    }

    /**
     * 일정 상세 조회
     * - registrations 도메인 붙기 전: participants/waiting/guests는 기본값으로 내려줌
     */
    fun getDetail(eventId: Long, requesterId: Long): EventDetailResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            NoSuchElementException("Event not found: $eventId")
        }

        val isCreator = event.createdBy == requesterId

        // 내 신청 정보(있으면)
        val myRegistration = registrationRepository.findByUserIdAndEventId(
            userId = requesterId,
            eventId = eventId,
        )

        val myRole = when {
            isCreator -> MyRole.CREATOR
            myRegistration != null && myRegistration.status != RegistrationStatus.CANCELED -> MyRole.PARTICIPANT
            else -> MyRole.NONE
        }

        val currentParticipants = registrationRepository.countByEventIdAndStatus(
            eventID = eventId,
            registrationStatus = RegistrationStatus.CONFIRMED,
        ).toInt()

        val waitingNum: Int? =
            if (myRegistration?.status == RegistrationStatus.WAITING) {
                val waitings = registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                    eventID = eventId,
                    registrationStatus = RegistrationStatus.WAITING,
                )
                val idx = waitings.indexOfFirst { it.id == myRegistration.id }
                if (idx >= 0) idx + 1 else null
            } else {
                null
            }

        // ✅ 참여자 미리보기: CONFIRMED 중 userId가 있는 애들만(예: 최대 5명)
        val confirmedRegs = registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
            eventID = eventId,
            registrationStatus = RegistrationStatus.CONFIRMED,
        )

        val userIds = confirmedRegs.mapNotNull { it.userId }.distinct().take(5)

        val usersById = userRepository.findAllById(userIds)
            .associateBy { it.id!! }

        val guestsPreview = userIds.mapNotNull { uid ->
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


    fun getById(eventId: Long): Event {
        return eventRepository.findById(eventId)
            .orElseThrow { NoSuchElementException("Event not found: $eventId") }
    }

    fun getByCreator(createdBy: Long): List<Event> {
        return eventRepository.findByCreatedByOrderByStartAtDesc(createdBy)
    }

    fun update(
        eventId: Long,
        title: String?,
        description: String?,
        location: String?,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean?,
        registrationStart: Instant?,
        registrationDeadline: Instant?,
    ): Event {
        val event = eventRepository.findById(eventId)
            .orElseThrow { NoSuchElementException("Event not found: $eventId") }

        // null은 "변경 없음"
        title?.let {
            require(it.isNotBlank()) { "title must not be blank" }
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

    fun delete(eventId: Long) {
        if (!eventRepository.existsById(eventId)) {
            throw NoSuchElementException("Event not found: $eventId")
        }
        eventRepository.deleteById(eventId)
    }

    private fun validateCreateOrUpdate(
        title: String,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        registrationStart: Instant?,
        registrationDeadline: Instant?,
    ) {
        require(title.isNotBlank()) { "title must not be blank" }

        if (startAt != null && endAt != null) {
            require(startAt.isBefore(endAt)) { "startAt must be before endAt" }
        }

        if (capacity != null) {
            require(capacity > 0) { "capacity must be positive" }
        }

        // registrationStart / registrationDeadline 관계
        if (registrationStart != null && registrationDeadline != null) {
            require(!registrationStart.isAfter(registrationDeadline)) {
                "registrationStart must be <= registrationDeadline"
            }
        }

        // registrationDeadline <= startAt (기존 룰 유지)
        if (registrationDeadline != null && startAt != null) {
            require(!registrationDeadline.isAfter(startAt)) {
                "registrationDeadline must be <= startAt"
            }
        }

        // (선택) registrationStart <= startAt 도 같이 강제하면 더 자연스러움
        if (registrationStart != null && startAt != null) {
            require(!registrationStart.isAfter(startAt)) {
                "registrationStart must be <= startAt"
            }
        }
    }
}
