package com.wafflestudio.spring2025.domain.event.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("events")
class Event(
    @Id var id: Long? = null,
    var title: String,
    var description: String? = null,
    var location: String? = null,
    var startAt: Instant? = null,
    var endAt: Instant? = null,
    var capacity: Int? = null,
    var waitlistEnabled: Boolean,
    var registrationDeadline: Instant? = null,
    var createdBy: Long,
    @org.springframework.data.annotation.CreatedDate
    var createdAt: Instant? = null,
    @org.springframework.data.annotation.LastModifiedDate
    var updatedAt: Instant? = null,
)
