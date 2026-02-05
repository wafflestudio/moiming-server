package com.wafflestudio.spring2025.domain.event.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.event.dto.request.CreateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.request.UpdateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.response.EventDetailResponse
import com.wafflestudio.spring2025.domain.event.dto.response.MyEventsInfiniteResponse
import com.wafflestudio.spring2025.domain.event.dto.response.UpdateEventResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

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
        val publicId: String =
            eventService.create(
                title = request.title,
                description = request.description,
                location = request.location,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
                capacity = request.capacity,
                registrationStartsAt = request.registrationStartsAt,
                registrationEndsAt = request.registrationEndsAt,
                waitlistEnabled = request.waitlistEnabled,
                createdBy = user.id!!,
            )

        return ResponseEntity
            .created(URI.create("/api/events/$publicId"))
            .build()
    }

    @Operation(summary = "이벤트 상세 조회", description = "이벤트 상세 정보를 조회합니다")
    @GetMapping("/{publicId}") // GET /api/events/{publicId}
    fun getById(
        @LoggedInUser user: User?, // nullable
        @PathVariable publicId: String,
    ): ResponseEntity<EventDetailResponse> {
        val response =
            eventService.getDetail(
                publicId = publicId,
                requesterId = user?.id, // Long?
            )
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "내가 만든 이벤트 목록 조회 (무한 스크롤)",
        description = "로그인된 사용자가 생성한 이벤트 목록을 무한 스크롤 방식(cursor)으로 조회합니다",
    )
    @GetMapping("/me")
    fun getMyEvents(
        @LoggedInUser user: User,
        @RequestParam(required = false) cursor: Instant?,
        @RequestParam(defaultValue = "5") size: Int,
    ): ResponseEntity<MyEventsInfiniteResponse> {
        val response =
            eventService.getMyEventsInfinite(
                createdBy = user.id!!,
                cursor = cursor,
                size = size,
            )
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "이벤트 수정", description = "이벤트를 수정합니다")
    @PutMapping("/{publicId}") // PUT /api/events/{publicId}
    fun update(
        @LoggedInUser user: User,
        @PathVariable publicId: String,
        @RequestBody request: UpdateEventRequest,
    ): ResponseEntity<UpdateEventResponse> {
        val event =
            eventService.update(
                publicId = publicId,
                title = request.title,
                description = request.description,
                location = request.location,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
                capacity = request.capacity,
                waitlistEnabled = request.waitlistEnabled,
                registrationStartsAt = request.registrationStartsAt,
                registrationEndsAt = request.registrationEndsAt,
                requesterId = user.id!!,
            )

        val response = UpdateEventResponse.from(event)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다")
    @DeleteMapping("/{publicId}") // DELETE /api/events/{publicId}
    fun delete(
        @LoggedInUser user: User,
        @PathVariable publicId: String,
    ): ResponseEntity<Unit> {
        eventService.delete(
            publicId = publicId,
            requesterId = user.id!!,
        )
        return ResponseEntity.noContent().build()
    }
}
