package com.wafflestudio.spring2025.domain.registration.service

interface WaitlistReconciliationService {
    fun reconcileWaitlist(eventId: Long)

    fun demoteToWaitlist(
        eventId: Long,
        newCapacity: Int,
    )
}
