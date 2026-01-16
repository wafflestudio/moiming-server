package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.auth.dto.SignupRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class AuthIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
    ) {
        @Test
        fun `should signup successfully`() {
            // 회원가입할 수 있다
            val email = "user1@example.com"
            val name = "user1"
            val password = "password"

            val request = SignupRequest(email, name, password)
            mvc
                .perform(
                    post("/api/v1/auth/register")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
        }

        @Test
        fun `should return 400 error when email is blank during signup`() {
            // 회원가입 시 이메일이 비어있으면 400 에러
            val request = SignupRequest("", "user", "password")
            mvc
                .perform(
                    post("/api/v1/auth/register")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 409 error when email already exists during signup`() {
            // 회원가입 시 이메일이 이미 존재하면 409 에러
            val (user, token) = dataGenerator.generateUser()
            val request = SignupRequest(user.email, user.name, "password", user.profileImage)
            mvc
                .perform(
                    post("/api/v1/auth/register")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
        }

        @Test
        fun `should login with email successfully`() {
            // 로그인할 수 있다
            val password = "myPassword"
            val (user, token) = dataGenerator.generateUserWithPassword(password)
            val request = LoginRequest(email = user.email, password = password)
            mvc
                .perform(
                    post("/api/v1/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
        }

        @Test
        fun `should return 401 error when email is incorrect during login`() {
            // 로그인 시 이메일이 틀렸다면 401 에러
            val request = LoginRequest(email = "wrong@example.com", password = "something")
            mvc
                .perform(
                    post("/api/v1/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `should retrieve own user information`() {
            // 본인 정보를 조회할 수 있다
            val (user, token) = dataGenerator.generateUser()
            mvc
                .perform(
                    get("/api/v1/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.email").value(user.email))
        }

        @Test
        fun `should return 401 when retrieving user information with invalid token`() {
            // 유효하지 않은 토큰으로 본인 정보 조회 시 401
            val (user, token) = dataGenerator.generateUser()
            mvc
                .perform(
                    get("/api/v1/users/me")
                        .header("Authorization", "Bearer wrong")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `should blacklist jwt`() {
            val (user, token) = dataGenerator.generateUser()
            mvc
                .perform(
                    get("/api/v1/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.email").value(user.email))
            mvc
                .perform(
                    post("/api/v1/users/logout")
                        .queryParam("token", token)
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
            mvc
                .perform(
                    post("/api/v1/users/logout")
                        .queryParam("token", token)
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
            mvc
                .perform(
                    get("/api/v1/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
        }
    }
