package com.wafflestudio.spring2025.domain.registration.repository

import com.wafflestudio.spring2025.domain.registration.model.RegistrationToken
import com.wafflestudio.spring2025.domain.registration.model.RegistrationTokenPurpose
import org.springframework.data.repository.ListCrudRepository

interface RegistrationTokenRepository : ListCrudRepository<RegistrationToken, Long> {
    fun findByTokenHashAndPurpose(tokenHash: String, purpose: RegistrationTokenPurpose): RegistrationToken?
    fun findByRegistrationId(registrationId: Long): List<RegistrationToken>
}
