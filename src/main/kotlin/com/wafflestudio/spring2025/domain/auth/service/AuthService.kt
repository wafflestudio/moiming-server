package com.wafflestudio.spring2025.domain.auth.service

import com.wafflestudio.spring2025.domain.auth.exception.AuthErrorCode
import com.wafflestudio.spring2025.domain.auth.exception.AuthValidationException
import com.wafflestudio.spring2025.domain.auth.exception.AuthenticationFailedException
import com.wafflestudio.spring2025.domain.auth.JwtTokenProvider
import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.identity.repository.UserIdentityRepository
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val identityRepository: UserIdentityRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtBlacklistService: JwtBlacklistService,
) {
    fun login(
        email: String,
        password: String,
    ): String {
        val user: User = userRepository.findByEmail(email) ?: throw AuthenticationFailedException()
        if (BCrypt.checkpw(password, user.passwordHash).not()) {
            throw AuthenticationFailedException()
        }
        val accessToken = jwtTokenProvider.createToken(user.id!!)
        return accessToken
    }

    fun logout(token: String?) {
        if (token != null) {
            jwtBlacklistService.addToBlacklist(token)
        }
    }

    fun signup(
        email: String,
        name: String,
        password: String,
        profileImage: String?,
    ): UserDto {
        validateEmail(email)
        validateName(name)
        validatePassword(password)

        // 이메일 인증 로직으로 인해 실제로 사용되지는 않음. (테스트용 함수)

        val user: User =
            userRepository.save(
                User(
                    email = email,
                    passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
                    name = name,
                    profileImage = profileImage,
                ),
            )

        return UserDto(user)
    }

    private fun validateEmail(email: String) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!email.matches(emailRegex)) {
            throw AuthValidationException(AuthErrorCode.BAD_EMAIL)
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw AuthValidationException(AuthErrorCode.BAD_NAME)
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
        if (!password.any { it.isLetter() }) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
        if (!password.any { it.isDigit() }) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
    }
}
