package com.wafflestudio.spring2025.domain.auth.service

import com.wafflestudio.spring2025.domain.auth.EmailAccountAlreadyExistsException
import com.wafflestudio.spring2025.domain.auth.JwtTokenProvider
import com.wafflestudio.spring2025.domain.auth.dto.LoginResponse
import com.wafflestudio.spring2025.domain.auth.external.client.OAuthClientRegistry
import com.wafflestudio.spring2025.domain.auth.external.dto.OAuthUserInfo
import com.wafflestudio.spring2025.domain.auth.model.SocialProvider
import com.wafflestudio.spring2025.domain.user.identity.model.UserIdentity
import com.wafflestudio.spring2025.domain.user.identity.repository.UserIdentityRepository
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class SocialAuthService(
    private val userRepository: UserRepository,
    private val identityRepository: UserIdentityRepository,
    private val oauthClientRegistry: OAuthClientRegistry,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private fun socialRegister(userInfo: OAuthUserInfo): UserIdentity {
        if (userRepository.existsByEmail(userInfo.email)) {
            throw EmailAccountAlreadyExistsException()
        }

        val user =
            userRepository.save(
                User(
                    email = userInfo.email,
                    name = userInfo.name,
                    profileImage = userInfo.profilePicture,
                    passwordHash = null,
                ),
            )

        return identityRepository.save(
            UserIdentity(
                userId = user.id!!,
                provider = userInfo.provider.name,
                providerUserId = userInfo.providerUserId,
            ),
        )
    }

    suspend fun socialLogin(
        provider: SocialProvider,
        code: String,
    ): LoginResponse {
        val oauthClient = oauthClientRegistry.getClient(provider)

        val userInfo =
            oauthClient.exchangeToken(code).let { token ->
                oauthClient.getUserInfo(token)
            }

        val identity =
            withContext(Dispatchers.IO) {
                identityRepository.findByProviderAndProviderUserId(
                    provider = provider.name,
                    providerUserId = userInfo.providerUserId,
                ) ?: socialRegister(userInfo)
            }

        val token = jwtTokenProvider.createToken(identity.userId)
        return LoginResponse(token = token)
    }
}
