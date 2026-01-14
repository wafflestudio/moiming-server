package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.CreateRegistrationResponse
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationTokenRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
class RegistrationIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository,
        private val registrationTokenRepository: RegistrationTokenRepository,
        private val jdbcTemplate: JdbcTemplate,
    ) {
        @Test
        fun `이벤트 신청 시 취소 토큰을 발급한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 1")

            val request = CreateRegistrationRequest(guestName = "guest", guestEmail = "guest@example.com")
            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(
                    status().isOk)
                .andExpect(jsonPath("$.registration.eventId").value(event.id))
                .andExpect(jsonPath("$.registration.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.cancelToken").isNotEmpty)

            val saved = registrationRepository.findByEventId(event.id!!).first()
            val tokens = registrationTokenRepository.findByRegistrationId(saved.id!!)
            assertThat(tokens).hasSize(1)
        }

        @Test
        fun `취소 토큰으로 신청을 취소한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 2")

            val request = CreateRegistrationRequest(guestName = "guest", guestEmail = "guest2@example.com")
            val createResult =
                mvc
                    .perform(
                        post("/api/v1/events/${event.id}/registrations")
                            .content(mapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val response =
                mapper.readValue(
                    createResult.response.contentAsString,
                    CreateRegistrationResponse::class.java,
                )

            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations/${response.registration.id}/cancel")
                        .queryParam("token", response.cancelToken)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            val canceled = registrationRepository.findById(response.registration.id).orElseThrow()
            assertThat(canceled.status.name).isEqualTo("CANCELED")
            assertThat(registrationTokenRepository.findByRegistrationId(canceled.id!!)).isEmpty()
        }

        @Test
        fun `내 신청 목록을 최신순과 페이징으로 조회한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event1 = createEvent(createdBy = user.id!!, title = "Event 1")
            val event2 = createEvent(createdBy = user.id!!, title = "Event 2")

            mvc
                .perform(
                    post("/api/v1/events/${event1.id}/registrations")
                        .content("{}")
                        .header("X-Test-User-Id", user.id.toString())
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            Thread.sleep(5)

            mvc
                .perform(
                    post("/api/v1/events/${event2.id}/registrations")
                        .content("{}")
                        .header("X-Test-User-Id", user.id.toString())
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            mvc
                .perform(
                    get("/api/v1/registrations/me")
                        .header("X-Test-User-Id", user.id.toString())
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event.id").value(event2.id))
                .andExpect(jsonPath("$[0].event.title").value(event2.title))
                .andExpect(jsonPath("$[0].registration.userId").value(user.id))
        }

        @Test
        fun `만료된 취소 토큰은 거절된다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 3")

            val request = CreateRegistrationRequest(guestName = "guest", guestEmail = "guest3@example.com")
            val createResult =
                mvc
                    .perform(
                        post("/api/v1/events/${event.id}/registrations")
                            .content(mapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val response =
                mapper.readValue(
                    createResult.response.contentAsString,
                    CreateRegistrationResponse::class.java,
                )

            val expiredAt = Instant.now().minus(Duration.ofHours(25))
            jdbcTemplate.update(
                "UPDATE registration_tokens SET created_at = ? WHERE registration_id = ?",
                Timestamp.from(expiredAt),
                response.registration.id,
            )

            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations/${response.registration.id}/cancel")
                        .queryParam("token", response.cancelToken)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isForbidden)

            val current = registrationRepository.findById(response.registration.id).orElseThrow()
            assertThat(current.status.name).isNotEqualTo("CANCELED")
        }

        @Test
        fun `확정 취소 시 대기자가 자동 승격된다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 4", capacity = 1, waitlistEnabled = true)

            val request1 = CreateRegistrationRequest(guestName = "guest1", guestEmail = "guest4a@example.com")
            val create1 =
                mvc
                    .perform(
                        post("/api/v1/events/${event.id}/registrations")
                            .content(mapper.writeValueAsString(request1))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            Thread.sleep(5)

            val request2 = CreateRegistrationRequest(guestName = "guest2", guestEmail = "guest4b@example.com")
            val create2 =
                mvc
                    .perform(
                        post("/api/v1/events/${event.id}/registrations")
                            .content(mapper.writeValueAsString(request2))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val response1 =
                mapper.readValue(
                    create1.response.contentAsString,
                    CreateRegistrationResponse::class.java,
                )

            val response2 =
                mapper.readValue(
                    create2.response.contentAsString,
                    CreateRegistrationResponse::class.java,
                )

            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations/${response1.registration.id}/cancel")
                        .queryParam("token", response1.cancelToken)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            val registrations = registrationRepository.findByEventId(event.id!!)
            val first = registrations.first { it.id == response1.registration.id }
            val second = registrations.first { it.id == response2.registration.id }

            assertThat(first.status.name).isEqualTo("CANCELED")
            assertThat(second.status.name).isEqualTo("CONFIRMED")
        }

        private fun createEvent(
            createdBy: Long,
            title: String,
            capacity: Int = 10,
            waitlistEnabled: Boolean = false,
        ): Event {
            return eventRepository.save(
                Event(
                    title = title,
                    description = null,
                    location = null,
                    startAt = Instant.now().plusSeconds(3600),
                    endAt = Instant.now().plusSeconds(7200),
                    capacity = capacity,
                    waitlistEnabled = waitlistEnabled,
                    registrationDeadline = null,
                    createdBy = createdBy,
                ),
            )
        }

        @TestConfiguration
        @Order(0)
        class TestWebConfig(
            private val userRepository: UserRepository,
        ) : WebMvcConfigurer {
            override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                resolvers.add(0, TestLoggedInUserArgumentResolver(userRepository))
            }
        }

        private class TestLoggedInUserArgumentResolver(
            private val userRepository: UserRepository,
        ) : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean {
                return parameter.hasParameterAnnotation(LoggedInUser::class.java)
            }

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?,
            ): Any? {
                val userIdHeader = webRequest.getHeader("X-Test-User-Id") ?: return null
                val userId = userIdHeader.toLongOrNull() ?: return null
                return userRepository.findById(userId).orElse(null)
            }
        }
    }
