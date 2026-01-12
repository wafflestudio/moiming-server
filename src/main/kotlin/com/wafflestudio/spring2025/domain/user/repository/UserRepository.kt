package com.wafflestudio.spring2025.domain.user.repository

import com.wafflestudio.spring2025.domain.user.model.User
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
