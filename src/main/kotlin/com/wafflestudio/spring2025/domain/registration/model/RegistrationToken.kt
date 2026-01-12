package com.wafflestudio.spring2025.domain.registration.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("registration_tokens")
class RegistrationToken(
    @Id var id: Long? = null,
    var registrationId: Long,
    var tokenHash: String,
    var purpose: RegistrationTokenPurpose,
    @CreatedDate
    var createdAt: Instant? = null,
)
