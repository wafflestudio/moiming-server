package com.wafflestudio.spring2025.domain.event.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 이벤트 상세 조회 시,
 * "viewer(요청자)"가 해당 이벤트와 어떤 관계에 있는지를 나타내는 상태
 */
@Schema(description = "이벤트 조회자(Viewer)의 상태")
enum class ViewerStatus {
    @Schema(description = "이벤트 생성자")
    HOST,

    @Schema(description = "참여 확정된 사용자")
    CONFIRMED,

    @Schema(description = "대기 중인 사용자")
    WAITLISTED,

    @Schema(description = "강제 취소(차단)된 사용자")
    BANNED,

    @Schema(description = "이벤트와 아무 관계 없는 사용자")
    NONE,
}
