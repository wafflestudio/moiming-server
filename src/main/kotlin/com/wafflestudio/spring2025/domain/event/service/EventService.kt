package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.domain.event.dto.core.EventDto
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EventService(
    private val eventRepository: EventRepository,
) {

    fun create(
        title: String,
        description: String?,
        location: String?,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean,
        registrationDeadline: Instant?,
        createdBy: Long,
    ): EventDto {
        validateCreateOrUpdate(
            title = title,
            startAt = startAt,
            endAt = endAt,
            capacity = capacity,
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
            registrationDeadline = registrationDeadline,
            createdBy = createdBy,
        )

        val saved = eventRepository.save(event)
        return EventDto(saved)
    }

    fun getById(eventId: Long): EventDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { NoSuchElementException("Event not found: $eventId") }
        return EventDto(event)
    }

    fun getByCreator(createdBy: Long): List<EventDto> {
        return eventRepository.findByCreatedByOrderByStartAtDesc(createdBy)
            .map { EventDto(it) }
    }

//    fun getUpcomingEvents(limit: Int = 3): List<EventDto> {
//        if (limit <= 0) return emptyList()
//
//        val now = Instant.now()
//
//        val events = when (limit) {
//            3 -> eventRepository.findTop3ByStartAtAfterOrderByStartAtAsc(now)
//            else -> eventRepository.findByStartAtAfterOrderByStartAtAsc(now).take(limit)
//        }
//
//        return events.map { EventDto(it) }
//    }

    fun update(
        eventId: Long,
        title: String?,
        description: String?,
        location: String?,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean?,
        registrationDeadline: Instant?,
    ): EventDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { NoSuchElementException("Event not found: $eventId") }

        // UpdateEventRequest에서 null은 "변경 없음"이라는 의미로 해석
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
        registrationDeadline?.let { event.registrationDeadline = it }

        // 변경 후에도 도메인 규칙 검증
        validateCreateOrUpdate(
            title = event.title,
            startAt = event.startAt,
            endAt = event.endAt,
            capacity = event.capacity,
            registrationDeadline = event.registrationDeadline,
        )

        val saved = eventRepository.save(event)
        return EventDto(saved)
    }

    fun delete(eventId: Long) {
        if (!eventRepository.existsById(eventId)) {
            throw NoSuchElementException("Event not found: $eventId")
        }
        eventRepository.deleteById(eventId)
    }

    fun registerUser(
        eventId: Long,
        userId: Long?,
    ) {
        // registrations 도메인(모델/레포지토리)이 필요해서 여기만으로 구현 불가
        // 보통 흐름:
        // 1) 이벤트 존재 확인
        // 2) 마감(registrationDeadline) 체크
        // 3) capacity/waitlist 정책 적용
        // 4) registrations insert (userId 또는 guest)
        TODO("registrations 도메인 연동 후 구현")
    }

    fun unregisterUser(
        eventId: Long,
        registrationId: Long,
    ) {
        // registrations 도메인(모델/레포지토리)이 필요해서 여기만으로 구현 불가
        // 보통 흐름:
        // 1) 이벤트 존재 확인
        // 2) registration 조회(해당 이벤트의 신청인지 확인)
        // 3) status 변경 또는 delete
        TODO("registrations 도메인 연동 후 구현")
    }

    private fun validateCreateOrUpdate(
        title: String,
        startAt: Instant?,
        endAt: Instant?,
        capacity: Int?,
        registrationDeadline: Instant?,
    ) {
        require(title.isNotBlank()) { "title must not be blank" }

        if (startAt != null && endAt != null) {
            require(startAt.isBefore(endAt)) { "startAt must be before endAt" }
        }

        if (registrationDeadline != null && startAt != null) {
            require(!registrationDeadline.isAfter(startAt)) {
                "registrationDeadline must be <= startAt"
            }
        }

        if (capacity != null) {
            require(capacity > 0) { "capacity must be positive" }
        }
    }
}
