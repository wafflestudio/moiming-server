package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.auth.dto.SignupRequest
import com.wafflestudio.spring2025.domain.auth.model.SocialProvider
import com.wafflestudio.spring2025.domain.user.identity.model.UserIdentity
import com.wafflestudio.spring2025.domain.user.identity.repository.UserIdentityRepository
import com.wafflestudio.spring2025.domain.user.model.PendingUser
import com.wafflestudio.spring2025.domain.user.repository.PendingUserRepository
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@Import(TestContainerConfig::class)
class AuthIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
        private val pendingUserRepository: PendingUserRepository,
        private val userIdentityRepository: UserIdentityRepository,
    ) {
        // 실제 이메일 발송 방지: EmailService를 Mock으로 대체
        @MockitoBean
        private lateinit var emailService: EmailService

        // =================================================================
        // POST /api/auth/signup — 이메일 회원가입 요청 (인증 이메일 발송 트리거)
        // =================================================================

        @Test
        fun `유효한 데이터로 회원가입 요청 시 204를 반환하고 인증 이메일을 발송한다`() {
            val email = "newuser-${UUID.randomUUID()}@example.com"
            val request =
                SignupRequest(
                    email = email,
                    name = "새사용자",
                    password = "password123",
                )

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)

            verify(emailService).sendVerificationEmail(eq(email), any())
        }

        @Test
        fun `이메일이 빈 문자열이면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "", name = "테스트유저", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_EMAIL"))
        }

        @Test
        fun `이메일 형식이 올바르지 않으면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "not-an-email", name = "테스트유저", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_EMAIL"))
        }

        @Test
        fun `이름이 공백이면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "user@example.com", name = "   ", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_NAME"))
        }

        @Test
        fun `비밀번호가 8자 미만이면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "user@example.com", name = "테스트유저", password = "pass1")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `비밀번호에 숫자가 없으면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "user@example.com", name = "테스트유저", password = "onlyletters")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `비밀번호에 문자가 없으면 회원가입 요청 시 400을 반환한다`() {
            val request = SignupRequest(email = "user@example.com", name = "테스트유저", password = "12345678")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `인증 대기 중인 이메일로 회원가입 요청 시 409를 반환한다`() {
            val email = "pending-${UUID.randomUUID()}@example.com"
            pendingUserRepository.save(
                PendingUser(
                    email = email,
                    name = "대기유저",
                    passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt()),
                    verificationCode = UUID.randomUUID().toString(),
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )

            val request = SignupRequest(email = email, name = "테스트유저", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_PENDING"))
        }

        @Test
        fun `이미 이메일로 가입된 계정에 회원가입 요청 시 409를 반환한다`() {
            val (user, _) = dataGenerator.generateUser()
            val request = SignupRequest(email = user.email, name = "테스트유저", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("EMAIL_ACCOUNT_ALREADY_EXIST"))
        }

        @Test
        fun `구글 소셜로 가입된 이메일로 회원가입 요청 시 409를 반환한다`() {
            val (user, _) = dataGenerator.generateUser()
            userIdentityRepository.save(
                UserIdentity(
                    userId = user.id!!,
                    provider = SocialProvider.GOOGLE.name,
                    providerUserId = "google-${UUID.randomUUID()}",
                ),
            )

            val request = SignupRequest(email = user.email, name = "테스트유저", password = "password123")

            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("GOOGLE_ACCOUNT_ALREADY_EXIST"))
        }

        // =================================================================
        // POST /api/auth/email-verification/{code} — 이메일 인증 완료
        // =================================================================

        @Test
        fun `유효한 인증 코드로 이메일 인증 성공 시 200을 반환한다`() {
            val verificationCode = UUID.randomUUID().toString()
            pendingUserRepository.save(
                PendingUser(
                    email = "verify-${UUID.randomUUID()}@example.com",
                    name = "인증유저",
                    passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt()),
                    verificationCode = verificationCode,
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )

            mvc
                .perform(
                    post("/api/auth/email-verification/$verificationCode"),
                ).andExpect(status().isOk)
        }

        @Test
        fun `존재하지 않는 인증 코드로 이메일 인증 시 400을 반환한다`() {
            val invalidCode = UUID.randomUUID().toString()

            mvc
                .perform(
                    post("/api/auth/email-verification/$invalidCode"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
        }

        @Test
        fun `만료된 인증 코드로 이메일 인증 시 400을 반환한다`() {
            val expiredCode = UUID.randomUUID().toString()
            pendingUserRepository.save(
                PendingUser(
                    email = "expired-${UUID.randomUUID()}@example.com",
                    name = "만료유저",
                    passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt()),
                    verificationCode = expiredCode,
                    expiresAt = Instant.now().minusSeconds(3600), // 이미 만료된 코드
                ),
            )

            mvc
                .perform(
                    post("/api/auth/email-verification/$expiredCode"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
        }

        // =================================================================
        // POST /api/auth/login — 이메일 로그인
        // =================================================================

        @Test
        fun `유효한 이메일과 비밀번호로 로그인 성공 시 200과 토큰을 반환한다`() {
            val password = "myPassword1"
            val (user, _) = dataGenerator.generateUserWithPassword(password)
            val request = LoginRequest(email = user.email, password = password)

            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isNotEmpty)
        }

        @Test
        fun `존재하지 않는 이메일로 로그인 시 401을 반환한다`() {
            val request = LoginRequest(email = "nonexistent@example.com", password = "password123")

            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
        }

        @Test
        fun `잘못된 비밀번호로 로그인 시 401을 반환한다`() {
            val (user, _) = dataGenerator.generateUserWithPassword("correctPassword1")
            val request = LoginRequest(email = user.email, password = "wrongPassword9")

            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
        }

        @Test
        fun `비밀번호 없이 생성된 사용자(소셜 가입)로 이메일 로그인 시 401을 반환한다`() {
            // DataGenerator.generateUser()는 passwordHash 없이 User 생성
            val (user, _) = dataGenerator.generateUser()
            val request = LoginRequest(email = user.email, password = "anyPassword1")

            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
        }

        // =================================================================
        // POST /api/auth/logout — 로그아웃 (JWT 블랙리스트 등록)
        // =================================================================

        @Test
        fun `유효한 토큰으로 로그아웃 성공 시 204를 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    post("/api/auth/logout")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)
        }

        @Test
        fun `Authorization 헤더 없이 로그아웃 요청 시 204를 반환한다`() {
            // /api/auth/logout은 @AuthRequired가 없으므로 인증 없이도 204 반환
            mvc
                .perform(
                    post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)
        }

        // =================================================================
        // GET /api/users/me — 로그아웃 블랙리스트 검증 (인증 흐름 관점)
        // 기본 내 정보 조회 시나리오는 UserIntegrationTest에서 다룸
        // =================================================================

        @Test
        fun `로그아웃 후 블랙리스트된 토큰으로 내 정보 조회 시 401을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            // 로그아웃으로 토큰 블랙리스트 등록
            mvc
                .perform(
                    post("/api/auth/logout")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)

            // 블랙리스트된 토큰으로 내 정보 조회 시 401
            mvc
                .perform(
                    get("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        // =================================================================
        // End-to-End: 회원가입 → 이메일 인증 → 로그인 → 내 정보 조회
        // =================================================================

        @Test
        fun `회원가입 요청 후 이메일 인증을 완료하면 로그인 후 내 정보를 조회할 수 있다`() {
            val email = "e2e-${UUID.randomUUID()}@example.com"
            val name = "E2E유저"
            val password = "password123"

            // 1. 회원가입 요청 → 204 No Content (인증 이메일 발송)
            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(SignupRequest(email = email, name = name, password = password)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)

            // 2. DB에서 PendingUser의 인증 코드 획득
            val verificationCode = pendingUserRepository.findByEmail(email)!!.verificationCode

            // 3. 이메일 인증 완료 → 200 OK (User 생성)
            mvc
                .perform(
                    post("/api/auth/email-verification/$verificationCode"),
                ).andExpect(status().isOk)

            // 4. 로그인 → 200 OK + JWT 토큰
            val loginResult =
                mvc
                    .perform(
                        post("/api/auth/login")
                            .content(mapper.writeValueAsString(LoginRequest(email = email, password = password)))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.token").isNotEmpty)
                    .andReturn()

            val token =
                mapper
                    .readTree(loginResult.response.contentAsString)
                    .get("token")
                    .asText()

            // 5. 내 정보 조회 → 200 OK + 사용자 정보
            mvc
                .perform(
                    get("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value(name))
        }

        @Test
        fun `이메일 인증 완료 후 동일한 인증 코드로 재인증 시도 시 400을 반환한다`() {
            val verificationCode = UUID.randomUUID().toString()
            pendingUserRepository.save(
                PendingUser(
                    email = "onetime-${UUID.randomUUID()}@example.com",
                    name = "일회용인증",
                    passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt()),
                    verificationCode = verificationCode,
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )

            // 첫 번째 인증 → 성공
            mvc
                .perform(
                    post("/api/auth/email-verification/$verificationCode"),
                ).andExpect(status().isOk)

            // 두 번째 인증 (PendingUser 삭제됨) → 실패
            mvc
                .perform(
                    post("/api/auth/email-verification/$verificationCode"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
        }

        // =================================================================
        // 이메일 인증 만료 후 재가입 요청 시나리오
        // EmailVerificationService.createPendingUser:
        //   만료된 PendingUser가 존재하면 삭제 후 새로운 PendingUser 생성
        // =================================================================

        @Test
        fun `인증 코드가 만료된 이메일로 재가입 요청 시 기존 PendingUser를 교체하고 204를 반환한다`() {
            val email = "reregister-${UUID.randomUUID()}@example.com"
            val oldCode = UUID.randomUUID().toString()

            // 만료된 PendingUser 사전 생성
            pendingUserRepository.save(
                PendingUser(
                    email = email,
                    name = "구사용자",
                    passwordHash = BCrypt.hashpw("oldPassword1", BCrypt.gensalt()),
                    verificationCode = oldCode,
                    expiresAt = Instant.now().minusSeconds(3600), // 이미 만료
                ),
            )

            // 동일 이메일로 재가입 요청 → 만료된 PendingUser 삭제 후 새로 생성 → 204
            val request = SignupRequest(email = email, name = "신사용자", password = "newPassword1")
            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)

            // 새로운 PendingUser가 생성됐는지 확인 (코드가 교체됨)
            val newPendingUser = pendingUserRepository.findByEmail(email)!!
            assertNotEquals(oldCode, newPendingUser.verificationCode)
            assertTrue(newPendingUser.expiresAt.isAfter(Instant.now()))
        }

        @Test
        fun `만료된 인증 코드로 인증 시도 후 동일 이메일로 재가입하면 새 코드로 인증할 수 있다`() {
            val email = "reauth-${UUID.randomUUID()}@example.com"
            val password = "password123"
            val expiredCode = UUID.randomUUID().toString()

            // 1. 만료된 PendingUser 사전 생성
            pendingUserRepository.save(
                PendingUser(
                    email = email,
                    name = "재인증유저",
                    passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
                    verificationCode = expiredCode,
                    expiresAt = Instant.now().minusSeconds(3600),
                ),
            )

            // 2. 만료된 코드로 인증 시도 → 400
            mvc
                .perform(
                    post("/api/auth/email-verification/$expiredCode"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))

            // 3. 동일 이메일로 재가입 요청 → 204 (만료된 PendingUser 삭제 후 신규 생성)
            mvc
                .perform(
                    post("/api/auth/signup")
                        .content(mapper.writeValueAsString(SignupRequest(email = email, name = "재인증유저", password = password)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNoContent)

            // 4. 새로운 인증 코드로 이메일 인증 → 200
            val newCode = pendingUserRepository.findByEmail(email)!!.verificationCode
            mvc
                .perform(
                    post("/api/auth/email-verification/$newCode"),
                ).andExpect(status().isOk)

            // 5. 새로 가입된 계정으로 로그인 → 200
            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(LoginRequest(email = email, password = password)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isNotEmpty)
        }
    }
