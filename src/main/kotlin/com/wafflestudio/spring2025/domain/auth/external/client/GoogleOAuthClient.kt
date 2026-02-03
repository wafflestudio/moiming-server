package com.wafflestudio.spring2025.domain.auth.external.client

import com.wafflestudio.spring2025.domain.auth.AuthenticateException
import com.wafflestudio.spring2025.domain.auth.external.dto.GoogleUserInfoResponse
import com.wafflestudio.spring2025.domain.auth.external.dto.OAuthUserInfo
import com.wafflestudio.spring2025.domain.auth.external.dto.TokenExchangeResponse
import com.wafflestudio.spring2025.domain.auth.model.SocialProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class GoogleOAuthClient(
    private val webClient: WebClient,
    @Value("\${oauth.providers.google.client-id}") private val clientId: String,
    @Value("\${oauth.providers.google.client-secret}") private val clientSecret: String,
    @Value("\${oauth.providers.google.redirect-uri}") private val redirectUri: String,
) : OAuthClient {
    override val provider = SocialProvider.GOOGLE

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val TOKEN_EXCHANGE_URL: String = "https://oauth2.googleapis.com/token"
        private val USER_INFO_URL: String = "https://www.googleapis.com/oauth2/v2/userinfo"
    }

    override suspend fun exchangeToken(code: String): String {
        logger.info("OAuth token exchange start: provider={}", provider)
        val body =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("code", code)
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("redirect_uri", redirectUri)
            }
        try {
            val response =
                webClient
                    .post()
                    .uri(TOKEN_EXCHANGE_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus({ it.isError }) {
                        it.bodyToMono<String>().map { errorBody ->
                            logger.warn(
                                "OAuth token exchange request failed: provider={}, status={}, errorBody={}",
                                provider,
                                it.statusCode().value(),
                                errorBody,
                            )
                            AuthenticateException()
                        }
                    }.awaitBody<TokenExchangeResponse>()
            logger.info("OAuth token exchange success: provider={}", provider)
            return response.accessToken
        } catch (e: WebClientResponseException) {
            // WebClient exception with status/body (body may include sensitive details; be careful)
            logger.warn(
                "OAuth token exchange WebClient error: provider={}, status={}, message={}",
                provider,
                e.statusCode.value(),
                e.message,
            )
            throw AuthenticateException()
        } catch (e: Exception) {
            logger.warn(
                "OAuth token exchange unexpected error: provider={}, type={}, message={}",
                provider,
                e.javaClass.simpleName,
                e.message,
            )
            throw AuthenticateException()
        }
    }

    private suspend fun getUserProfileEmail(token: String): GoogleUserInfoResponse {
        logger.info("getting user profile with provider = {}", provider)
        try {
            return webClient
                .get()
                .uri(USER_INFO_URL)
                .header("Authorization", "Bearer $token")
                .retrieve()
                .onStatus({ it.isError }) {
                    it.bodyToMono<String>().map { errorBody ->
                        logger.warn(
                            "OAuth userinfo request failed: provider={}, status={}, errorBody={}",
                            provider,
                            it.statusCode().value(),
                            errorBody,
                        )
                        AuthenticateException()
                    }
                }.awaitBody<GoogleUserInfoResponse>()
        } catch (e: Exception) {
            throw AuthenticateException()
        }
    }

    override suspend fun getUserInfo(token: String): OAuthUserInfo {
        val userProfile = getUserProfileEmail(token)
        return OAuthUserInfo(
            provider = SocialProvider.GOOGLE,
            providerUserId = userProfile.id,
            name = userProfile.name,
            email = userProfile.email,
            profilePicture = userProfile.picture,
        )
    }
}
