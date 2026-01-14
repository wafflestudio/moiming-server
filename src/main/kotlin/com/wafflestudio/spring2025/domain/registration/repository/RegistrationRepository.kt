package com.wafflestudio.spring2025.domain.registration.repository

import com.wafflestudio.spring2025.domain.registration.model.Registration
import org.springframework.data.repository.ListCrudRepository

interface RegistrationRepository : ListCrudRepository<Registration, Long> {
    fun findByEventId(eventId: Long): List<Registration>

    fun findByUserId(userId: Long): List<Registration>

    fun findByUserIdAndEventId(
        userId: Long,
        eventId: Long,
    ): Registration?

    fun existsByUserIdAndEventId(
        userId: Long,
        eventId: Long,
    ): Boolean
}
