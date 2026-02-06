package com.wafflestudio.spring2025.domain.registration.repository

import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface RegistrationRepository :
    ListCrudRepository<Registration, Long>,
    PagingAndSortingRepository<Registration, Long> {
    fun findByEventId(eventId: Long): List<Registration>

    fun findByRegistrationPublicId(registrationPublicId: String): Registration?

    fun findByUserIdOrderByCreatedAtDesc(
        userId: Long,
        pageable: Pageable,
    ): List<Registration>

    fun findByUserIdAndEventId(
        userId: Long,
        eventId: Long,
    ): Registration?

    fun findByGuestEmailAndEventId(
        guestEmail: String,
        eventId: Long,
    ): Registration?

    fun countByEventIdAndStatus(
        eventID: Long,
        registrationStatus: RegistrationStatus,
    ): Long

    fun findByEventIdAndStatusOrderByCreatedAtAsc(
        eventID: Long,
        registrationStatus: RegistrationStatus,
    ): List<Registration>

    @Query(
        """
        SELECT event_id AS eventId, COUNT(*) AS totalCount
        FROM registrations
        WHERE event_id IN (:eventIds) AND status IN (:statuses)
        GROUP BY event_id
        """,
    )
    fun countByEventIdsAndStatuses(
        eventIds: Collection<Long>,
        statuses: Collection<RegistrationStatus>,
    ): List<EventRegistrationCount>

    @Query(
        """
        SELECT registration_public_id AS registrationPublicId,
               event_id AS eventId,
               ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY created_at ASC, id ASC) AS waitlistNumber
        FROM registrations
        WHERE event_id IN (:eventIds) AND status = :status
        """,
    )
    fun findWaitlistPositionsByEventIds(
        eventIds: Collection<Long>,
        status: RegistrationStatus,
    ): List<WaitlistPosition>
}

data class EventRegistrationCount(
    val eventId: Long,
    val totalCount: Long,
)

data class WaitlistPosition(
    val registrationPublicId: String,
    val eventId: Long,
    val waitlistNumber: Long,
)
