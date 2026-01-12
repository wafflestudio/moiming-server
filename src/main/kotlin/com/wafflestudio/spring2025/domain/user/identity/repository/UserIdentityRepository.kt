package com.wafflestudio.spring2025.domain.user.identity.repository

import com.wafflestudio.spring2025.domain.user.identity.model.UserIdentity
import org.springframework.data.repository.ListCrudRepository

interface UserIdentityRepository : ListCrudRepository<UserIdentity, Long> {
    fun findByProviderAndProviderUserId(provider: String, providerUserId: String): UserIdentity?
    fun findByUserId(userId: Long): List<UserIdentity>
}
