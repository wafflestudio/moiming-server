package com.wafflestudio.spring2025.domain.auth.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("user_identities")
class UserIdentity(
    @Id
    var id: Long? = null,
    @Column("user_id")
    var userId: Long,
    val provider: String,
    @CreatedDate
    @Column("created_at")
    var createdAt: Instant? = null,
)
