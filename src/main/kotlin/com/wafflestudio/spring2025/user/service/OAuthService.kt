package com.wafflestudio.spring2025.user.service

import com.wafflestudio.spring2025.user.AuthenticateException
import com.wafflestudio.spring2025.user.JwtTokenProvider
import com.wafflestudio.spring2025.user.UnregisteredSocialAccountException
import com.wafflestudio.spring2025.user.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class OAuthService (
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
){
    fun authenticate(
        provider: String,
        authCode: String,
    ): String {
        // TODO
        if (false) { // FIXME
            throw UnregisteredSocialAccountException()
        }
        val username = "" // FIXME
        val accessToken = jwtTokenProvider.createToken(username) // FIXME
        return accessToken
    }
}
