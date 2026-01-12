package com.wafflestudio.spring2025.domain.event.repository

import com.wafflestudio.spring2025.domain.event.model.Event
import org.springframework.data.repository.ListCrudRepository
import java.time.Instant

interface EventRepository : ListCrudRepository<Event, Long> {
    fun findByCreatedByOrderByStartAtDesc(createdBy: Long): List<Event>
    fun findByStartAtAfterOrderByStartAtAsc(now: Instant): List<Event>
    fun findTop3ByStartAtAfterOrderByStartAtAsc(now: Instant): List<Event>
}
