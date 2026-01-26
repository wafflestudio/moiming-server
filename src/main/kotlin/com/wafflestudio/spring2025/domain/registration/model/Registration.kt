package com.wafflestudio.spring2025.domain.registration.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("registrations")
class Registration(
    @Id var id: Long? = null,
    @Column("registration_public_id")
    var registrationPublicId: String = UUID.randomUUID().toString(),
    var userId: Long? = null,
    var eventId: Long,
    var guestName: String? = null,
    var guestEmail: String? = null,
    var status: RegistrationStatus,
    @CreatedDate
    var createdAt: Instant? = null,
)
