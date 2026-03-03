package com.wafflestudio.spring2025.domain.event.dto.response

import com.wafflestudio.spring2025.domain.event.model.Event
import java.time.Instant

data class UpdateEventResponse(
    val title: String,
    val description: String?,
    val location: String?,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val capacity: Int?,
    val waitlistEnabled: Boolean,
    val registrationStartsAt: Instant?,
    val registrationEndsAt: Instant?,
    val notice: String,
) {
    companion object {
        const val PARTICIPANT_NOTICE: String =
            "수정된 사항은 참여자들에게 자동 안내되지 않습니다. 필요 시 직접 안내해 주세요."

        fun from(event: Event): UpdateEventResponse =
            UpdateEventResponse(
                title = event.title,
                description = event.description,
                location = event.location,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                capacity = event.capacity,
                waitlistEnabled = event.waitlistEnabled,
                registrationStartsAt = event.registrationStartsAt,
                registrationEndsAt = event.registrationEndsAt,
                notice = PARTICIPANT_NOTICE,
            )
    }
}
