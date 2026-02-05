package com.wafflestudio.spring2025.domain.event.repository

import com.wafflestudio.spring2025.domain.event.model.Event
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.ListCrudRepository
import java.time.Instant

interface EventRepository : ListCrudRepository<Event, Long> {
    fun findByPublicId(publicId: String): Event?

    fun existsByPublicId(publicId: String): Boolean

    fun findByCreatedByOrderByStartAtDesc(createdBy: Long): List<Event>

    fun findByStartAtAfterOrderByStartAtAsc(now: Instant): List<Event>

    fun findTop3ByStartAtAfterOrderByStartAtAsc(now: Instant): List<Event>

    fun findByCreatedByAndCreatedAtIsNotNullOrderByCreatedAtDesc(
        createdBy: Long,
        pageable: Pageable,
    ): List<Event>

    // 다음 페이지: cursor 이전(더 과거)
    fun findByCreatedByAndCreatedAtIsNotNullAndCreatedAtLessThanOrderByCreatedAtDesc(
        createdBy: Long,
        cursor: Instant,
        pageable: Pageable,
    ): List<Event>
}
