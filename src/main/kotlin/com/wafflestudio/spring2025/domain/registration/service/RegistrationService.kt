package com.wafflestudio.spring2025.domain.registration.service

import com.wafflestudio.spring2025.domain.registration.dto.core.RegistrationDto
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import org.springframework.stereotype.Service

@Service
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
) {
    fun create(userId: Long?, eventId: Long, guestName: String?, guestEmail: String?): RegistrationDto {
        TODO("이벤트 신청 생성 구현")
    }

    fun getByEventId(eventId: Long): List<RegistrationDto> {
        TODO("이벤트별 신청 목록 조회 구현")
    }

    fun confirm(registrationId: Long) {
        TODO("신청 승인 (상태 변경) 구현")
    }

    fun cancel(registrationId: Long) {
        TODO("신청 취소 (상태 변경) 구현")
    }
}
