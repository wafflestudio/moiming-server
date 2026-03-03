package com.wafflestudio.spring2025

import com.wafflestudio.spring2025.domain.event.dto.response.UpdateEventResponse
import com.wafflestudio.spring2025.domain.event.model.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UpdateEventResponseTest {
    @Test
    fun `이벤트 수정 응답에 안내 문구가 포함된다`() {
        val event =
            Event(
                publicId = UUID.randomUUID().toString(),
                title = "title",
                description = "description",
                location = "location",
                startsAt = Instant.now().plusSeconds(3600),
                endsAt = Instant.now().plusSeconds(7200),
                capacity = 10,
                waitlistEnabled = true,
                registrationStartsAt = Instant.now().plusSeconds(600),
                registrationEndsAt = Instant.now().plusSeconds(1800),
                createdBy = 1L,
            )

        val response = UpdateEventResponse.from(event)

        assertThat(response.notice).isEqualTo(UpdateEventResponse.PARTICIPANT_NOTICE)
    }
}
