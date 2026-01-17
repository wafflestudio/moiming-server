package com.wafflestudio.spring2025.domain.event.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.event.dto.request.CreateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.request.UpdateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.response.EventDetailResponse
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.service.EventService
import com.wafflestudio.spring2025.domain.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/events")
@Tag(name = "Event", description = "이벤트 관리 API")
class EventController(
    private val eventService: EventService,
) {
    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다")
    @PostMapping // POST /api/events
    fun create(
        @LoggedInUser user: User,
        @RequestBody request: CreateEventRequest,
    ): ResponseEntity<Void> {
        val eventId: Long =
            eventService.create(
                title = request.title,
                description = request.description,
                location = request.location,
                startAt = request.startAt,
                endAt = request.endAt,
                capacity = request.capacity,
                registrationStart = request.registrationStart,
                registrationDeadline = request.registrationDeadline,
                waitlistEnabled = request.waitlistEnabled,
                createdBy = user.id!!,
            )

        // response body 비움 + 생성된 리소스 위치(Location) 제공
        // 204(No content)를 보낼 수도 있지만, 이게 좀 더 명확함
        return ResponseEntity
            .created(URI.create("/api/events/$eventId"))
            .build()
    }

    @Operation(summary = "이벤트 상세 조회", description = "이벤트 상세 정보를 조회합니다")
    @GetMapping("/{id}") // GET /api/events/{id}
    fun getById(
        @LoggedInUser user: User,
        @PathVariable("id") eventId: Long,
    ): ResponseEntity<EventDetailResponse> {
        val response =
            eventService.getDetail(
                eventId = eventId,
                requesterId = user.id!!,
            )
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "이벤트 수정", description = "이벤트를 수정합니다")
    @PutMapping("/{id}") // PUT /api/events/{id}
    fun update(
        @LoggedInUser user: User, // ✅ 인증만 확인
        @PathVariable("id") eventId: Long,
        @RequestBody request: UpdateEventRequest,
    ): ResponseEntity<Event?> {
        val dto =
            eventService.update(
                eventId = eventId,
                title = request.title,
                description = request.description,
                location = request.location,
                startAt = request.startAt,
                endAt = request.endAt,
                capacity = request.capacity,
                waitlistEnabled = request.waitlistEnabled,
                registrationStart = request.registrationStart,
                registrationDeadline = request.registrationDeadline,
            )
        return ResponseEntity.ok(dto)
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다")
    @DeleteMapping("/{id}") // DELETE /api/events/{id}
    fun delete(
        @LoggedInUser user: User, // ✅ 토큰 유효성만 확인
        @PathVariable("id") eventId: Long,
    ): ResponseEntity<Unit> {
        eventService.delete(eventId)
        return ResponseEntity.noContent().build()
    }
}
