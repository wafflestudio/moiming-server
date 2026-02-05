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
) {
    companion object {
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
            )
    }
}
