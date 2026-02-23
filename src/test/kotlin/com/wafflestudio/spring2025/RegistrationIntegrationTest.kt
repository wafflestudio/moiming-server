package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.domain.auth.LoggedInUser
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
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
    ) {
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
                .andExpect(jsonPath("$.registrations.length()").value(1))
                .andExpect(jsonPath("$.registrations[0].title").value(event2.title))
                .andExpect(jsonPath("$.registrations[0].publicId").value(event2.publicId))
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
                    startsAt = Instant.now().plusSeconds(3600),
                    endsAt = Instant.now().plusSeconds(7200),
                    capacity = capacity,
                    waitlistEnabled = waitlistEnabled,
                    registrationEndsAt = null,
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
