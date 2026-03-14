package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.dto.request.CreateRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.request.DeleteRegistrationRequest
import com.wafflestudio.spring2025.domain.registration.dto.request.UpdateRegistrationStatusRequest
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@Import(TestContainerConfig::class)
class RegistrationIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository,
    ) {
        @MockitoBean
        private lateinit var emailService: EmailService

        @Test
        fun `로그인 사용자가 등록하면 CONFIRMED 상태로 저장된다`() {
            val (host, _) = dataGenerator.generateUser()
            val (participant, participantToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Backend Study", capacity = 10, waitlistEnabled = false)

            val result =
                mvc
                    .perform(
                        post("/api/events/${event.publicId}/registrations")
                            .header("Authorization", "Bearer $participantToken")
                            .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.registrationPublicId").isNotEmpty)
                    .andReturn()

            val registrationId = extractRegistrationPublicId(result)
            val saved = findRegistration(registrationId)

            assertEquals(participant.id, saved.userId)
            assertNull(saved.guestName)
            assertNull(saved.guestEmail)
            assertEquals(RegistrationStatus.CONFIRMED, saved.status)
        }

        @Test
        fun `비로그인 사용자도 guestName과 guestEmail로 등록할 수 있다`() {
            val (host, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Guest Registration")

            val registrationId = registerAsGuest(event.publicId, "Guest User", "guest-1@example.com")
            val saved = findRegistration(registrationId)

            assertNull(saved.userId)
            assertEquals("Guest User", saved.guestName)
            assertEquals("guest-1@example.com", saved.guestEmail)
            assertEquals(RegistrationStatus.CONFIRMED, saved.status)
        }

        @Test
        fun `주최자는 자신의 이벤트에 등록할 수 없다`() {
            val (host, hostToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Host Event")

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .header("Authorization", "Bearer $hostToken")
                        .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("REGISTRATION_BLOCKED_HOST"))
        }

        @Test
        fun `정원이 가득 찼고 대기열이 비활성화면 등록을 할 수 없다`() {
            val (host, _) = dataGenerator.generateUser()
            val (_, firstRegistrantToken) = dataGenerator.generateUser()
            val (_, secondRegistrantToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "No Waitlist", capacity = 1, waitlistEnabled = false)

            registerAsUser(event.publicId, firstRegistrantToken)

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .header("Authorization", "Bearer $secondRegistrantToken")
                        .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("EVENT_FULL"))
        }

        @Test
        fun `정원이 가득 찼고 대기열이 활성화면 WAITLISTED로 등록된다`() {
            val (host, _) = dataGenerator.generateUser()
            val (_, confirmedUserToken) = dataGenerator.generateUser()
            val (_, waitlistedUserToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Waitlist Enabled", capacity = 1, waitlistEnabled = true)

            val confirmedRegistrationId = registerAsUser(event.publicId, confirmedUserToken)
            val waitlistedRegistrationId = registerAsUser(event.publicId, waitlistedUserToken)

            assertEquals(RegistrationStatus.CONFIRMED, findRegistration(confirmedRegistrationId).status)
            assertEquals(RegistrationStatus.WAITLISTED, findRegistration(waitlistedRegistrationId).status)
        }

        @Test
        fun `이벤트 신청 시작 시간 이전에는 신청할 수 없다`() {
            val (host, _) = dataGenerator.generateUser()
            val (_, participantToken) = dataGenerator.generateUser()
            val now = Instant.now()
            val event =
                createEvent(
                    createdBy = host.id!!,
                    title = "Registration Not Open Yet",
                    registrationStartsAt = now.plusSeconds(1800),
                    registrationEndsAt = now.plusSeconds(7200),
                )

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .header("Authorization", "Bearer $participantToken")
                        .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("NOT_WITHIN_REGISTRATION_WINDOW"))
        }

        @Test
        fun `이벤트 신청 마감 시간 이후에는 신청할 수 없다`() {
            val (host, _) = dataGenerator.generateUser()
            val (_, participantToken) = dataGenerator.generateUser()
            val now = Instant.now()
            val event =
                createEvent(
                    createdBy = host.id!!,
                    title = "Registration Closed",
                    registrationStartsAt = now.minusSeconds(7200),
                    registrationEndsAt = now.minusSeconds(60),
                )

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .header("Authorization", "Bearer $participantToken")
                        .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("NOT_WITHIN_REGISTRATION_WINDOW"))
        }

        @Test
        fun `게스트 등록 시 이메일 형식이 잘못되면 400을 반환한다`() {
            val (host, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Invalid Guest Email")

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .content(
                            mapper.writeValueAsString(
                                CreateRegistrationRequest(
                                    guestName = "Guest User",
                                    guestEmail = "invalid-email-format",
                                ),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_WRONG_EMAIL"))
        }

        @Test
        fun `같은 이벤트에 동일한 게스트 이메일로 중복 등록할 수 없다`() {
            val (host, _) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Duplicate Guest Email")

            registerAsGuest(event.publicId, "Guest One", "guest-duplicate@example.com")

            mvc
                .perform(
                    post("/api/events/${event.publicId}/registrations")
                        .content(
                            mapper.writeValueAsString(
                                CreateRegistrationRequest(
                                    guestName = "Guest Two",
                                    guestEmail = "guest-duplicate@example.com",
                                ),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_EXISTS"))
        }

        @Test
        fun `본인의 참가 신청은 본인만 취소할 수 있다`() {
            val (host, hostToken) = dataGenerator.generateUser()
            val (_, participantToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Host Delete Permission")
            val participantRegistrationId = registerAsUser(event.publicId, participantToken)

            mvc
                .perform(
                    delete("/api/registrations/$participantRegistrationId")
                        .header("Authorization", "Bearer $hostToken")
                        .content(mapper.writeValueAsString(DeleteRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("REGISTRATION_DELETE_UNAUTHORIZED"))

            mvc
                .perform(
                    delete("/api/registrations/$participantRegistrationId")
                        .header("Authorization", "Bearer $participantToken")
                        .content(mapper.writeValueAsString(DeleteRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            assertNull(registrationRepository.findByRegistrationPublicId(participantRegistrationId))
        }

        @Test
        fun `CONFIRMED 등록을 삭제하면 대기자가 승격된다`() {
            val (host, _) = dataGenerator.generateUser()
            val (_, confirmedUserToken) = dataGenerator.generateUser()
            val (_, waitlistedUserToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Delete Promotion", capacity = 1, waitlistEnabled = true)

            val confirmedRegistrationId = registerAsUser(event.publicId, confirmedUserToken)
            val waitlistedRegistrationId = registerAsUser(event.publicId, waitlistedUserToken)

            assertEquals(RegistrationStatus.CONFIRMED, findRegistration(confirmedRegistrationId).status)
            assertEquals(RegistrationStatus.WAITLISTED, findRegistration(waitlistedRegistrationId).status)

            mvc
                .perform(
                    delete("/api/registrations/$confirmedRegistrationId")
                        .header("Authorization", "Bearer $confirmedUserToken")
                        .content(mapper.writeValueAsString(DeleteRegistrationRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)

            assertNull(registrationRepository.findByRegistrationPublicId(confirmedRegistrationId))
            assertEquals(RegistrationStatus.CONFIRMED, findRegistration(waitlistedRegistrationId).status)
        }

        @Test
        fun `주최자만 강퇴할 수 있고 CONFIRMED 강퇴 시 대기자가 승격된다`() {
            val (host, hostToken) = dataGenerator.generateUser()
            val (confirmedUser, confirmedUserToken) = dataGenerator.generateUser()
            val (_, waitlistedUserToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Ban Scenario", capacity = 1, waitlistEnabled = true)

            val confirmedRegistrationId = registerAsUser(event.publicId, confirmedUserToken)
            val waitlistedRegistrationId = registerAsUser(event.publicId, waitlistedUserToken)

            mvc
                .perform(
                    patch("/api/registrations/$confirmedRegistrationId")
                        .header("Authorization", "Bearer $confirmedUserToken")
                        .content(
                            mapper.writeValueAsString(
                                UpdateRegistrationStatusRequest(status = RegistrationStatus.BANNED),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("REGISTRATION_PATCH_UNAUTHORIZED"))

            mvc
                .perform(
                    patch("/api/registrations/$confirmedRegistrationId")
                        .header("Authorization", "Bearer $hostToken")
                        .content(
                            mapper.writeValueAsString(
                                UpdateRegistrationStatusRequest(status = RegistrationStatus.BANNED),
                            ),
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.patchEmail").value(confirmedUser.email))

            assertEquals(RegistrationStatus.BANNED, findRegistration(confirmedRegistrationId).status)
            assertEquals(RegistrationStatus.CONFIRMED, findRegistration(waitlistedRegistrationId).status)
        }

        @Test
        fun `주최자만 참여자 이메일을 볼 수 있고 참여자는 볼 수 없다`() {
            val (host, hostToken) = dataGenerator.generateUser()
            val (participant, participantToken) = dataGenerator.generateUser()
            val event = createEvent(createdBy = host.id!!, title = "Email Visibility", capacity = 10, waitlistEnabled = false)

            registerAsUser(event.publicId, participantToken)
            registerAsGuest(event.publicId, "Guest User", "guest@example.com")

            val nonHostResponse =
                mvc
                    .perform(
                        get("/api/events/${event.publicId}/registrations")
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val nonHostParticipants = extractParticipants(nonHostResponse)
            assertTrue(nonHostParticipants.all { it.path("email").isNull })

            val hostResponse =
                mvc
                    .perform(
                        get("/api/events/${event.publicId}/registrations")
                            .header("Authorization", "Bearer $hostToken")
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andReturn()

            val hostParticipants = extractParticipants(hostResponse)
            assertTrue(hostParticipants.any { it.path("email").asText() == participant.email })
            assertTrue(hostParticipants.any { it.path("email").asText() == "guest@example.com" })
        }

        @Test
        fun `동시에 여러 사용자가 신청해도 정원을 초과하지 않는다`() {
            val capacity = 4
            val totalUsers = 15

            val (host, _) = dataGenerator.generateUser()
            val event =
                createEvent(
                    createdBy = host.id!!,
                    title = "동시성 테스트 이벤트",
                    capacity = capacity,
                    waitlistEnabled = true,
                )

            val tokens = (1..totalUsers).map { dataGenerator.generateUser().second }

            val executor = Executors.newFixedThreadPool(totalUsers)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(totalUsers)

            tokens.forEach { token ->
                executor.submit {
                    try {
                        startLatch.await()
                        mvc.perform(
                            post("/api/events/${event.publicId}/registrations")
                                .header("Authorization", "Bearer $token")
                                .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                                .contentType(MediaType.APPLICATION_JSON),
                        )
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            startLatch.countDown()
            doneLatch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            val confirmed = registrationRepository.countByEventIdAndStatus(event.id!!, RegistrationStatus.CONFIRMED)
            val waitlisted = registrationRepository.countByEventIdAndStatus(event.id!!, RegistrationStatus.WAITLISTED)

            assertEquals(capacity.toLong(), confirmed, "CONFIRMED 수가 정원과 일치해야 합니다")
            assertEquals((totalUsers - capacity).toLong(), waitlisted, "WAITLISTED 수가 정확해야 합니다")
        }

        private fun registerAsUser(
            eventPublicId: String,
            token: String,
        ): String {
            val result =
                mvc
                    .perform(
                        post("/api/events/$eventPublicId/registrations")
                            .header("Authorization", "Bearer $token")
                            .content(mapper.writeValueAsString(CreateRegistrationRequest()))
                            .contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.registrationPublicId").isNotEmpty)
                    .andReturn()

            return extractRegistrationPublicId(result)
        }

        private fun registerAsGuest(
            eventPublicId: String,
            guestName: String,
            guestEmail: String,
        ): String {
            val result =
                mvc
                    .perform(
                        post("/api/events/$eventPublicId/registrations")
                            .content(
                                mapper.writeValueAsString(
                                    CreateRegistrationRequest(
                                        guestName = guestName,
                                        guestEmail = guestEmail,
                                    ),
                                ),
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.registrationPublicId").isNotEmpty)
                    .andReturn()

            return extractRegistrationPublicId(result)
        }

        private fun extractRegistrationPublicId(result: MvcResult): String =
            mapper.readTree(result.response.contentAsString).path("registrationPublicId").asText()

        private fun extractParticipants(result: MvcResult): List<JsonNode> =
            mapper
                .readTree(result.response.contentAsString)
                .path("participants")
                .toList()

        private fun findRegistration(registrationPublicId: String): Registration {
            val registration = registrationRepository.findByRegistrationPublicId(registrationPublicId)
            assertNotNull(registration)
            return registration!!
        }

        private fun createEvent(
            createdBy: Long,
            title: String,
            capacity: Int = 10,
            waitlistEnabled: Boolean = false,
            registrationStartsAt: Instant? = null,
            registrationEndsAt: Instant? = null,
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
                    registrationStartsAt = registrationStartsAt,
                    registrationEndsAt = registrationEndsAt,
                    createdBy = createdBy,
                ),
            )
    }
