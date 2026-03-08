package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.common.image.service.ImageService
import com.wafflestudio.spring2025.domain.auth.dto.LoginRequest
import com.wafflestudio.spring2025.domain.user.dto.PatchMeRequest
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@Import(TestContainerConfig::class)
class UserIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
    ) {
        // UserService.validateProfileImage 에서 직접 호출하는 S3Client 모킹
        @MockitoBean
        private lateinit var s3Client: S3Client

        // UserService.me 에서 presignedGetUrl 호출 시 실제 AWS 호출 방지
        @MockitoBean
        private lateinit var imageService: ImageService

        // =================================================================
        // GET /api/users/me — 내 정보 조회
        // =================================================================

        @Test
        fun `유효한 토큰으로 내 정보 조회 시 200과 사용자 정보를 반환한다`() {
            val (user, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    get("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(user.id))
                .andExpect(jsonPath("$.email").value(user.email))
                .andExpect(jsonPath("$.name").value(user.name))
                .andExpect(jsonPath("$.profileImage").value(null as Any?))
        }

        @Test
        fun `Authorization 헤더 없이 내 정보 조회 시 401을 반환한다`() {
            mvc
                .perform(
                    get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `유효하지 않은 토큰으로 내 정보 조회 시 401을 반환한다`() {
            mvc
                .perform(
                    get("/api/users/me")
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `프로필 이미지가 있는 사용자의 내 정보 조회 시 presigned URL을 반환한다`() {
            val s3Key = "profile-images/1/photo.jpg"
            val presignedUrl = "https://s3.example.com/presigned/photo.jpg"

            // profileImage가 설정된 User를 DB에 직접 생성
            val (user, token) = dataGenerator.generateUserWithProfileImage(s3Key)

            given(imageService.presignedGetUrl(s3Key)).willReturn(presignedUrl)

            mvc
                .perform(
                    get("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(user.id))
                .andExpect(jsonPath("$.profileImage").value(presignedUrl))
        }

        // =================================================================
        // PATCH /api/users/me — 내 정보 수정
        // =================================================================

        @Test
        fun `이름을 변경하면 200과 변경된 이름을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val newName = "변경된이름"

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(PatchMeRequest(name = newName, email = null, password = null, profileImage = null)),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(newName))
        }

        @Test
        fun `변경 필드 없이 PATCH 시 기존 정보 그대로 200을 반환한다`() {
            val (user, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(PatchMeRequest(name = null, email = null, password = null, profileImage = null)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(user.name))
                .andExpect(jsonPath("$.email").value(user.email))
        }

        @Test
        fun `비밀번호를 변경하면 이전 비밀번호는 무효화되고 새 비밀번호로 로그인할 수 있다`() {
            val oldPassword = "oldPassword1"
            val newPassword = "newPassword2"
            val (user, token) = dataGenerator.generateUserWithPassword(oldPassword)

            // 비밀번호 변경
            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = null, email = null, password = newPassword, profileImage = null),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            // 이전 비밀번호로 로그인 → 실패
            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(LoginRequest(email = user.email, password = oldPassword)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)

            // 새 비밀번호로 로그인 → 성공
            mvc
                .perform(
                    post("/api/auth/login")
                        .content(mapper.writeValueAsString(LoginRequest(email = user.email, password = newPassword)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isNotEmpty)
        }

        @Test
        fun `S3에 존재하는 키로 프로필 이미지를 변경하면 200을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val validKey = "profile-images/test/valid.jpg"
            val presignedUrl = "https://s3.example.com/presigned/valid.jpg"

            // headObject 는 기본적으로 예외 없이 반환 (mock 기본값) → 유효한 키로 간주
            given(imageService.presignedGetUrl(validKey)).willReturn(presignedUrl)

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(PatchMeRequest(name = null, email = null, password = null, profileImage = validKey)),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.profileImage").value(presignedUrl))
        }

        @Test
        fun `이름과 비밀번호를 동시에 변경하면 200과 변경된 정보를 반환한다`() {
            val oldPassword = "oldPassword1"
            val newPassword = "newPassword2"
            val newName = "새이름"
            val (user, token) = dataGenerator.generateUserWithPassword(oldPassword)

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = newName, email = null, password = newPassword, profileImage = null),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.email").value(user.email))
        }

        @Test
        fun `이메일 변경 시도 시 403을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = null, email = "newemail@example.com", password = null, profileImage = null),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("EMAIL_CHANGE_FORBIDDEN"))
        }

        @Test
        fun `이름을 공백 문자열로 변경 시도 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(PatchMeRequest(name = "   ", email = null, password = null, profileImage = null)),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_NAME"))
        }

        @Test
        fun `비밀번호를 8자 미만으로 변경 시도 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(PatchMeRequest(name = null, email = null, password = "abc1", profileImage = null)),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `비밀번호에 숫자가 없으면 변경 시도 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = null, email = null, password = "onlyletters", profileImage = null),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `비밀번호에 문자가 없으면 변경 시도 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = null, email = null, password = "12345678", profileImage = null),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("BAD_PASSWORD"))
        }

        @Test
        fun `S3에 존재하지 않는 키로 프로필 이미지 변경 시도 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            given(s3Client.headObject(any(HeadObjectRequest::class.java)))
                .willThrow(
                    NoSuchKeyException
                        .builder()
                        .message("Not found")
                        .statusCode(404)
                        .build(),
                )

            mvc
                .perform(
                    patch("/api/users/me")
                        .header("Authorization", "Bearer $token")
                        .content(
                            mapper.writeValueAsString(
                                PatchMeRequest(name = null, email = null, password = null, profileImage = "nonexistent/image.jpg"),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("PROFILE_IMAGE_NOT_FOUND"))
        }

        @Test
        fun `토큰 없이 내 정보 수정 시도 시 401을 반환한다`() {
            mvc
                .perform(
                    patch("/api/users/me")
                        .content(
                            mapper.writeValueAsString(PatchMeRequest(name = "새이름", email = null, password = null, profileImage = null)),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }
    }
