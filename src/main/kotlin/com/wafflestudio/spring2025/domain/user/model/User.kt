package com.wafflestudio.spring2025.domain.user.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("users")
class User(
    @Id var id: Long? = null,
    var email: String,
    var name: String,
    var profileImage: String? = null,
    @CreatedDate
    var createdAt: Instant? = null,
    @LastModifiedDate
    var updatedAt: Instant? = null,
)
