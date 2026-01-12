package com.wafflestudio.spring2025.domain.event.controller

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.event.dto.CreateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.UpdateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.core.EventDto
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
import java.time.Instant

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event", description = "이벤트 관리 API")
class EventController(
    private val eventService: EventService,
) {
    @Operation(summary = "이벤트 생성", description = "새로운 이벤트를 생성합니다")
    @PostMapping
    fun create(
        @LoggedInUser user: User,
        @RequestBody request: CreateEventRequest,
    ): ResponseEntity<EventDto> {
        TODO("이벤트 생성 API 구현")
    }

    @Operation(summary = "이벤트 목록 조회", description = "작성자 기준 이벤트 목록을 조회합니다")
    @GetMapping
    fun list(@LoggedInUser user: User): ResponseEntity<List<EventDto>> {
        TODO("이벤트 목록 조회 API 구현")
    }

    @Operation(summary = "이벤트 상세 조회", description = "이벤트 상세 정보를 조회합니다")
    @GetMapping("/{eventId}")
    fun getById(
        @PathVariable eventId: Long,
    ): ResponseEntity<EventDto> {
        TODO("이벤트 상세 조회 API 구현")
    }

    @Operation(summary = "이벤트 수정", description = "이벤트를 수정합니다")
    @PutMapping("/{eventId}")
    fun update(
        @PathVariable eventId: Long,
        @RequestBody request: UpdateEventRequest,
    ): ResponseEntity<EventDto> {
        TODO("이벤트 수정 API 구현")
    }

    @Operation(summary = "이벤트 삭제", description = "이벤트를 삭제합니다")
    @DeleteMapping("/{eventId}")
    fun delete(
        @PathVariable eventId: Long,
    ): ResponseEntity<Unit> {
        TODO("이벤트 삭제 API 구현")
    }
}
