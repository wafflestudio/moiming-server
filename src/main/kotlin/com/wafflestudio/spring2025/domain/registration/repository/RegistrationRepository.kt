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

    @Query(
        """
        SELECT r.*
        FROM registrations r
        WHERE r.registration_public_id = :registrationPublicId
        FOR UPDATE
        """,
    )
    fun lockByRegistrationPublicId(registrationPublicId: String): Registration?

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

    fun countByEventId(eventID: Long): Long

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

    @Query(
        """
        SELECT r.*
        FROM registrations r
        WHERE r.event_id = :eventId
        ORDER BY r.created_at DESC, r.registration_public_id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPageByEventIdOrderByCreatedAtAsc(
        eventId: Long,
        limit: Int,
        offset: Int,
    ): List<Registration>

    @Query(
        """
        SELECT r.*
        FROM registrations r
        WHERE r.event_id = :eventId AND r.status = :status
        ORDER BY r.created_at DESC, r.registration_public_id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPageByEventIdAndStatusOrderByCreatedAtAsc(
        eventId: Long,
        status: RegistrationStatus,
        limit: Int,
        offset: Int,
    ): List<Registration>

    @Query(
        """
        SELECT r.*
        FROM registrations r
        LEFT JOIN users u ON r.user_id = u.id
        WHERE r.event_id = :eventId
        ORDER BY LOWER(COALESCE(r.guest_name, u.name, '')) ASC, r.registration_public_id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPageByEventIdOrderByNameAsc(
        eventId: Long,
        limit: Int,
        offset: Int,
    ): List<Registration>

    @Query(
        """
        SELECT r.*
        FROM registrations r
        LEFT JOIN users u ON r.user_id = u.id
        WHERE r.event_id = :eventId AND r.status = :status
        ORDER BY LOWER(COALESCE(r.guest_name, u.name, '')) ASC, r.registration_public_id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPageByEventIdAndStatusOrderByNameAsc(
        eventId: Long,
        status: RegistrationStatus,
        limit: Int,
        offset: Int,
    ): List<Registration>

    @Query(
        """
        SELECT ranked.registration_public_id AS registration_public_id,
               ranked.waitlist_number AS waitlist_number
        FROM (
            SELECT r.registration_public_id,
                   ROW_NUMBER() OVER (ORDER BY r.created_at ASC, r.registration_public_id ASC) AS waitlist_number
            FROM registrations r
            WHERE r.event_id = :eventId AND r.status = :status
        ) ranked
        WHERE ranked.registration_public_id IN (:registrationPublicIds)
        """,
    )
    fun findWaitlistPositionsByRegistrationPublicIds(
        eventId: Long,
        status: RegistrationStatus,
        registrationPublicIds: Collection<String>,
    ): List<WaitlistPositionByPublicId>
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

data class WaitlistPositionByPublicId(
    val registrationPublicId: String,
    val waitlistNumber: Long,
)
