package com.wafflestudio.spring2025.domain.event.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class EventLockRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun lockIdByPublicId(publicId: String): Long? =
        jdbcTemplate.query(
            "SELECT id FROM events WHERE public_id = ? FOR UPDATE",
            { rs, _ -> rs.getLong("id") },
            publicId,
        ).firstOrNull()

    fun lockById(eventId: Long): Boolean {
        val result =
            jdbcTemplate.query(
                "SELECT id FROM events WHERE id = ? FOR UPDATE",
                { rs, _ -> rs.getLong("id") },
                eventId,
            )
        return result.isNotEmpty()
    }
}
