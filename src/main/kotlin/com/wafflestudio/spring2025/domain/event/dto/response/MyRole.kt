package com.wafflestudio.spring2025.domain.event.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 요청자 역할
 * - CREATOR: 이벤트 생성자
 * - PARTICIPANT: 이벤트 참여자 (CONFIRMED / WAITING)
 * - NONE: 로그인은 했지만 참여하지 않음
 */
@Schema(description = "요청자 역할")
enum class MyRole {
    @Schema(description = "이벤트 생성자")
    CREATOR,

    @Schema(description = "이벤트 참여자")
    PARTICIPANT,

    @Schema(description = "이벤트 미참여자")
    NONE,
}
