package com.wafflestudio.spring2025.domain.registration.repository

import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import org.springframework.data.repository.ListCrudRepository

interface RegistrationRepository : ListCrudRepository<Registration, Long> {
    fun findByEventId(eventId: Long): List<Registration>

    fun findByRegistrationPublicId(registrationPublicId: String): Registration?

    fun findByUserId(userId: Long): List<Registration>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Registration>

    fun findByUserIdAndEventId(
        userId: Long,
        eventId: Long,
    ): Registration?

    fun findByGuestEmailAndEventId(
        guestEmail: String,
        eventId: Long,
    ): Registration?

    fun existsByUserIdAndEventId(
        userId: Long,
        eventId: Long,
    ): Boolean

    fun existsByGuestEmailAndEventId(
        guestEmail: String,
        eventId: Long,
    ): Boolean

    fun countByEventId(eventID: Long): Long

    fun countByEventIdAndStatus(
        eventID: Long,
        registrationStatus: RegistrationStatus,
    ): Long

    fun findByEventIdAndStatusOrderByCreatedAtAsc(
        eventID: Long,
        registrationStatus: RegistrationStatus,
    ): List<Registration>
}
