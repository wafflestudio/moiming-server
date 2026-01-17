package com.wafflestudio.spring2025.domain.user.service

import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun me(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): UserDto {
        return UserDto(user = user)
    }
}
