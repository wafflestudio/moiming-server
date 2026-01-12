package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.domain.event.dto.core.EventDto
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
        TODO("일정 생성 구현")
    }

    fun getById(eventId: Long): EventDto {
        TODO("일정 조회 구현")
    }

    fun getByCreator(createdBy: Long): List<EventDto> {
        TODO("작성자별 일정 목록 조회 구현")
    }

    fun getUpcomingEvents(limit: Int = 3): List<EventDto> {
        TODO("다가오는 일정 조회 구현")
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
        registrationDeadline: Instant?,
    ): EventDto {
        TODO("일정 수정 구현")
    }

    fun delete(eventId: Long) {
        TODO("일정 삭제 구현")
    }

    fun registerUser(eventId: Long, userId: Long?) {
        TODO("이벤트 신청 구현")
    }

    fun unregisterUser(eventId: Long, registrationId: Long) {
        TODO("이벤트 신청 취소 구현")
    }
}
