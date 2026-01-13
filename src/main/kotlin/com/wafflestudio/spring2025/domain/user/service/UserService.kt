package com.wafflestudio.spring2025.domain.user.service

import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun me(): UserDto {
        TODO("본인 정보 조회")
    }
}
