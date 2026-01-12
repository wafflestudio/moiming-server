package com.wafflestudio.spring2025.domain.user.identity.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("user_identities")
class UserIdentity(
    @Id var id: Long? = null,
    var userId: Long,
    var provider: String,
    var providerUserId: String,
    @CreatedDate
    var createdAt: Instant? = null,
)
