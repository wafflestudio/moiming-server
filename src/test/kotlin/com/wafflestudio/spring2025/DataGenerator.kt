package com.wafflestudio.spring2025

import com.wafflestudio.spring2025.domain.auth.JwtTokenProvider
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DataGenerator(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun generateUser(): Pair<User, String> {
        val email = "user-${UUID.randomUUID()}@example.com"
        val user =
            userRepository.save(
                User(
                    email = email,
                    name = "user",
                    profileImage = null,
                ),
            )
        val token = jwtTokenProvider.createToken(user.email)
        return user to token
    }
    fun generateUserWithPassword(password: String = "testPassword123"): Pair<User, String> {
        val email = "user-${UUID.randomUUID()}@example.com"
        val user = userRepository.save(
            User(
                email = email,
                name = "user",
                passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
                profileImage = null,
            ),
        )
        val token = jwtTokenProvider.createToken(user.email)
        return user to token
    }
}
