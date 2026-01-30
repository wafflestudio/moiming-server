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
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
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
import java.time.Instant
import java.util.UUID

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
    ) {
        @Test
        fun `이벤트 신청 시 상태와 이메일을 반환하고 취소 토큰을 저장한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 1")

            val request = CreateRegistrationRequest(guestName = "guest", guestEmail = "guest@example.com")
            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(
                    status().isOk,
                ).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.waitingNum").value(nullValue()))
                .andExpect(jsonPath("$.confirmEmail").value("guest@example.com"))

            val saved = registrationRepository.findByEventId(event.id!!).first()
            val tokens = registrationTokenRepository.findByRegistrationId(saved.id!!)
            assertThat(tokens).hasSize(1)
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
        fun `대기 신청 시 상태와 대기 순번을 반환한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = user.id!!, title = "Event 4", capacity = 1, waitlistEnabled = true)

            val request1 = CreateRegistrationRequest(guestName = "guest1", guestEmail = "guest4a@example.com")
            mvc
                .perform(
                    post("/api/v1/events/${event.id}/registrations")
                        .content(mapper.writeValueAsString(request1))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            val request2 = CreateRegistrationRequest(guestName = "guest2", guestEmail = "guest4b@example.com")
            val create2 =
                mvc
                    .perform(
                        post("/api/v1/events/${event.id}/registrations")
                            .content(mapper.writeValueAsString(request2))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val response2 =
                mapper.readValue(
                    create2.response.contentAsString,
                    CreateRegistrationResponse::class.java,
                )

            assertThat(response2.status.name).isEqualTo("WAITING")
            assertThat(response2.waitingNum).isEqualTo(1)
            assertThat(response2.confirmEmail).isEqualTo("guest4b@example.com")
        }

        private fun createEvent(
            createdBy: Long,
            title: String,
            capacity: Int = 10,
            waitlistEnabled: Boolean = false,
        ): Event =
            eventRepository.save(
                Event(
                    publicId = UUID.randomUUID().toString(),
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
            override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.hasParameterAnnotation(LoggedInUser::class.java)

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
