package com.wafflestudio.spring2025.domain.user.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("pending_users")
class PendingUser(
    @Id var id: Long? = null,
    var email: String,
    var name: String,
    var passwordHash: String,
    var profileImage: String? = null,
    var verificationCode: String,
    @CreatedDate
    var createdAt: Instant? = null,
    var expiresAt: Instant,
)
