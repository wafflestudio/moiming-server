package com.wafflestudio.spring2025.domain.event.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("events")
class Event(
    @Id
    var id: Long? = null,
    var title: String,
    var description: String? = null,
    var location: String? = null,
    @Column("start_at")
    var startAt: Instant? = null,
    @Column("end_at")
    var endAt: Instant? = null,
    var capacity: Int? = null,
    @Column("waitlist_enabled")
    var waitlistEnabled: Boolean,
    @Column("registration_start")
    var registrationStart: Instant? = null,
    @Column("registration_deadline")
    var registrationDeadline: Instant? = null,
    @Column("created_by")
    var createdBy: Long,
    @CreatedDate
    @Column("created_at")
    var createdAt: Instant? = null,
    @LastModifiedDate
    @Column("updated_at")
    var updatedAt: Instant? = null,
)
