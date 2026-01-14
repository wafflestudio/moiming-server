package com.wafflestudio.spring2025.domain.user.repository

import com.wafflestudio.spring2025.domain.user.model.PendingUser
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface PendingUserRepository : CrudRepository<PendingUser, Long> {
    fun findByEmail(email: String): PendingUser?

    fun findByVerificationCode(verificationCode: String): PendingUser?

    fun existsByEmail(email: String): Boolean

    fun deleteByExpiresAtBefore(expiresAt: Instant)
}
