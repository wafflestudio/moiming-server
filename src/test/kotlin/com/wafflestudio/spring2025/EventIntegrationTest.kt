package com.wafflestudio.spring2025

import com.fasterxml.jackson.databind.ObjectMapper
import com.wafflestudio.spring2025.domain.event.dto.request.CreateEventRequest
import com.wafflestudio.spring2025.domain.event.dto.request.UpdateEventRequest
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class EventIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val mapper: ObjectMapper,
        private val dataGenerator: DataGenerator,
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository,
    ) {
        // =================================================================
        // Helpers
        // =================================================================

        private fun createEventInDb(
            createdBy: Long,
            title: String = "테스트 이벤트",
            capacity: Int = 10,
            waitlistEnabled: Boolean = false,
            startsAt: Instant = Instant.now().plusSeconds(3600),
            endsAt: Instant = Instant.now().plusSeconds(7200),
            registrationStartsAt: Instant? = null,
            registrationEndsAt: Instant? = null,
        ): Event =
            eventRepository.save(
                Event(
                    publicId = UUID.randomUUID().toString(),
                    title = title,
                    capacity = capacity,
                    waitlistEnabled = waitlistEnabled,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    registrationStartsAt = registrationStartsAt,
                    registrationEndsAt = registrationEndsAt,
                    createdBy = createdBy,
                ),
            )

        /**
         * 유효한 CreateEventRequest 기본값.
         * - startsAt=now+7200, endsAt=now+10800 (미래)
         * - registrationStartsAt=now+3600 < startsAt(+7200) ✓
         * - registrationEndsAt=now+5400  < startsAt(+7200), > registrationStartsAt(+3600) ✓
         */
        private fun validCreateRequest(
            title: String = "정기 모임",
            capacity: Int = 30,
            waitlistEnabled: Boolean = false,
        ) = CreateEventRequest(
            title = title,
            description = "정기 모임입니다",
            location = "강의실 101",
            startsAt = Instant.now().plusSeconds(7200),
            endsAt = Instant.now().plusSeconds(10800),
            capacity = capacity,
            waitlistEnabled = waitlistEnabled,
            registrationStartsAt = Instant.now().plusSeconds(3600),
            registrationEndsAt = Instant.now().plusSeconds(5400),
        )

        // =================================================================
        // POST /api/events — 이벤트 생성
        // =================================================================

        @Test
        fun `유효한 데이터로 이벤트 생성 시 200과 publicId를 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(validCreateRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.publicId").isNotEmpty)
        }

        @Test
        fun `인증 없이 이벤트 생성 요청 시 401을 반환한다`() {
            mvc
                .perform(
                    post("/api/events")
                        .content(mapper.writeValueAsString(validCreateRequest()))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `제목이 공백인 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(validCreateRequest(title = "   ")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_TITLE_BLANK"))
        }

        @Test
        fun `정원이 0인 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(validCreateRequest(capacity = 0)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_INVALID"))
        }

        @Test
        fun `정원이 음수인 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(validCreateRequest(capacity = -5)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_INVALID"))
        }

        @Test
        fun `모임 시작 시간이 과거이면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    startsAt = Instant.now().minusSeconds(3600),
                    endsAt = Instant.now().plusSeconds(3600),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_STARTS_IN_PAST"))
        }

        @Test
        fun `모임 종료 시간이 과거이면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    startsAt = null,
                    endsAt = Instant.now().minusSeconds(3600),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_ENDS_IN_PAST"))
        }

        @Test
        fun `모임 시작 시간이 종료 시간보다 늦으면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    startsAt = Instant.now().plusSeconds(7200),
                    endsAt = Instant.now().plusSeconds(3600), // startsAt 보다 이전
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_TIME_RANGE_INVALID"))
        }

        @Test
        fun `신청 시작 시간이 과거이면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    registrationStartsAt = Instant.now().minusSeconds(3600),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_STARTS_IN_PAST"))
        }

        @Test
        fun `신청 마감 시간이 과거이면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    registrationEndsAt = Instant.now().minusSeconds(3600),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_ENDS_IN_PAST"))
        }

        @Test
        fun `신청 마감 시간이 신청 시작 시간보다 이르면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    registrationStartsAt = Instant.now().plusSeconds(5400),
                    registrationEndsAt = Instant.now().plusSeconds(3600), // 시작보다 이전
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_TIME_RANGE_INVALID"))
        }

        @Test
        fun `신청 시작 시간이 모임 시작 시간보다 늦으면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            // startsAt=+3600, registrationStartsAt=+5400 → 신청 시작이 모임 시작보다 늦음
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    startsAt = Instant.now().plusSeconds(3600),
                    endsAt = Instant.now().plusSeconds(7200),
                    registrationStartsAt = Instant.now().plusSeconds(5400),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_STARTS_AFTER_EVENT_START"))
        }

        @Test
        fun `신청 마감 시간이 모임 시작 시간보다 늦으면 이벤트 생성 요청 시 400을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()
            // startsAt=+3600, registrationEndsAt=+5400 → 신청 마감이 모임 시작보다 늦음
            val request =
                CreateEventRequest(
                    title = "정기 모임",
                    capacity = 10,
                    waitlistEnabled = false,
                    startsAt = Instant.now().plusSeconds(3600),
                    endsAt = Instant.now().plusSeconds(7200),
                    registrationEndsAt = Instant.now().plusSeconds(5400),
                )

            mvc
                .perform(
                    post("/api/events")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("REGISTRATION_ENDS_AFTER_EVENT_START"))
        }

        // =================================================================
        // GET /api/events/{publicId} — 이벤트 상세 조회
        // =================================================================

        @Test
        fun `존재하지 않는 publicId로 이벤트 상세 조회 시 404를 반환한다`() {
            mvc
                .perform(
                    get("/api/events/${UUID.randomUUID()}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
        }

        @Test
        fun `비로그인 사용자가 이벤트 상세 조회 시 200과 올바른 응답 구조를 반환한다`() {
            val (creator, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!, title = "공개 이벤트", capacity = 10)

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.event.publicId").value(event.publicId))
                .andExpect(jsonPath("$.event.title").value("공개 이벤트"))
                .andExpect(jsonPath("$.event.capacity").value(10))
                .andExpect(jsonPath("$.event.totalApplicants").value(0))
                .andExpect(jsonPath("$.creator.name").value(creator.name))
                .andExpect(jsonPath("$.creator.email").value(creator.email))
                .andExpect(jsonPath("$.viewer.status").value("NONE"))
                .andExpect(jsonPath("$.viewer.name").value(null as Any?))
                .andExpect(jsonPath("$.viewer.waitlistPosition").value(null as Any?))
                .andExpect(jsonPath("$.capabilities.shareLink").value(false))
                .andExpect(jsonPath("$.capabilities.apply").value(true)) // 정원 여유 있고 신청 기간 제한 없음
                .andExpect(jsonPath("$.capabilities.wait").value(false))
                .andExpect(jsonPath("$.capabilities.cancel").value(false))
                .andExpect(jsonPath("$.guestsPreview").isArray)
        }

        @Test
        fun `로그인한 이벤트 주최자가 상세 조회 시 viewerStatus가 HOST이고 shareLink가 true이다`() {
            val (host, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = host.id!!)

            mvc
                .perform(
                    get("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.viewer.status").value("HOST"))
                .andExpect(jsonPath("$.viewer.name").value(host.name))
                .andExpect(jsonPath("$.capabilities.shareLink").value(true))
                .andExpect(jsonPath("$.capabilities.apply").value(false))
                .andExpect(jsonPath("$.capabilities.wait").value(false))
                .andExpect(jsonPath("$.capabilities.cancel").value(false))
        }

        @Test
        fun `로그인했지만 참여하지 않은 사용자가 이벤트 조회 시 viewerStatus가 NONE이고 이름이 포함된다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (viewer, viewerToken) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!)

            mvc
                .perform(
                    get("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $viewerToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.viewer.status").value("NONE"))
                .andExpect(jsonPath("$.viewer.name").value(viewer.name))
        }

        @Test
        fun `정원이 찬 이벤트를 비로그인 사용자가 조회 시 apply는 false이다`() {
            val (creator, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!, capacity = 1, waitlistEnabled = false)

            registrationRepository.save(
                Registration(
                    userId = creator.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.CONFIRMED,
                ),
            )

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.capabilities.apply").value(false))
                .andExpect(jsonPath("$.capabilities.wait").value(false))
        }

        @Test
        fun `정원이 찬 이벤트에서 waitlistEnabled이면 비로그인 사용자 조회 시 wait가 true이다`() {
            val (creator, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!, capacity = 1, waitlistEnabled = true)

            registrationRepository.save(
                Registration(
                    userId = creator.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.CONFIRMED,
                ),
            )

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.capabilities.apply").value(false))
                .andExpect(jsonPath("$.capabilities.wait").value(true))
        }

        @Test
        fun `신청 기간이 지난 이벤트를 비로그인 사용자가 조회 시 apply와 wait 모두 false이다`() {
            val (creator, _) = dataGenerator.generateUser()
            // registrationEndsAt이 과거 → withinWindow = false
            val event =
                eventRepository.save(
                    Event(
                        publicId = UUID.randomUUID().toString(),
                        title = "마감된 이벤트",
                        capacity = 10,
                        waitlistEnabled = true,
                        startsAt = Instant.now().plusSeconds(3600),
                        endsAt = Instant.now().plusSeconds(7200),
                        registrationStartsAt = Instant.now().minusSeconds(7200),
                        registrationEndsAt = Instant.now().minusSeconds(3600), // 이미 마감
                        createdBy = creator.id!!,
                    ),
                )

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.capabilities.apply").value(false))
                .andExpect(jsonPath("$.capabilities.wait").value(false))
        }

        @Test
        fun `CONFIRMED 참가자가 이벤트 조회 시 viewerStatus가 CONFIRMED이고 cancel이 true이다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (participant, participantToken) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!)

            registrationRepository.save(
                Registration(
                    userId = participant.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.CONFIRMED,
                ),
            )

            mvc
                .perform(
                    get("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $participantToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.viewer.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.capabilities.cancel").value(true))
                .andExpect(jsonPath("$.capabilities.apply").value(false))
        }

        @Test
        fun `WAITLISTED 참가자가 이벤트 조회 시 viewerStatus가 WAITLISTED이고 waitlistPosition이 포함된다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (waiter, waiterToken) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!, capacity = 1, waitlistEnabled = true)

            // 정원 채우기 (1명 확정)
            registrationRepository.save(
                Registration(
                    userId = creator.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.CONFIRMED,
                ),
            )
            // 대기 등록
            registrationRepository.save(
                Registration(
                    userId = waiter.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.WAITLISTED,
                ),
            )

            mvc
                .perform(
                    get("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $waiterToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.viewer.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.viewer.waitlistPosition").value(1))
                .andExpect(jsonPath("$.capabilities.cancel").value(true))
        }

        @Test
        fun `totalApplicants는 CONFIRMED + WAITLISTED 수의 합이다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (user1, _) = dataGenerator.generateUser()
            val (user2, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!, capacity = 2, waitlistEnabled = true)

            registrationRepository.save(Registration(userId = creator.id!!, eventId = event.id!!, status = RegistrationStatus.CONFIRMED))
            registrationRepository.save(Registration(userId = user1.id!!, eventId = event.id!!, status = RegistrationStatus.CONFIRMED))
            registrationRepository.save(Registration(userId = user2.id!!, eventId = event.id!!, status = RegistrationStatus.WAITLISTED))

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.event.totalApplicants").value(3))
        }

        // =================================================================
        // GET /api/events/me — 내가 만든 이벤트 목록 조회 (무한 스크롤)
        // =================================================================

        @Test
        fun `인증 없이 내 이벤트 목록 조회 요청 시 401을 반환한다`() {
            mvc
                .perform(
                    get("/api/events/me"),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `생성한 이벤트가 없을 때 내 이벤트 목록 조회 시 빈 목록을 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    get("/api/events/me")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.events").isArray)
                .andExpect(jsonPath("$.events.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false))
        }

        @Test
        fun `내 이벤트 목록 조회 시 내가 생성한 이벤트만 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val (otherUser, _) = dataGenerator.generateUser()

            val myEvent = createEventInDb(createdBy = user.id!!, title = "내 이벤트")
            createEventInDb(createdBy = otherUser.id!!, title = "다른 사람 이벤트")

            mvc
                .perform(
                    get("/api/events/me")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].publicId").value(myEvent.publicId))
                .andExpect(jsonPath("$.events[0].title").value("내 이벤트"))
        }

        @Test
        fun `내 이벤트 목록 조회 시 size 파라미터로 페이지 크기를 제한할 수 있다`() {
            val (user, token) = dataGenerator.generateUser()
            repeat(3) { i -> createEventInDb(createdBy = user.id!!, title = "이벤트 $i") }

            mvc
                .perform(
                    get("/api/events/me")
                        .header("Authorization", "Bearer $token")
                        .param("size", "2"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty)
        }

        @Test
        fun `내 이벤트 목록에 이벤트 응답 필드가 올바르게 포함된다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!, title = "필드 검증 이벤트", capacity = 20)

            mvc
                .perform(
                    get("/api/events/me")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.events[0].publicId").value(event.publicId))
                .andExpect(jsonPath("$.events[0].title").value("필드 검증 이벤트"))
                .andExpect(jsonPath("$.events[0].capacity").value(20))
                .andExpect(jsonPath("$.events[0].totalApplicants").value(0))
        }

        @Test
        fun `cursor 기반 페이징으로 다음 페이지를 조회할 수 있다`() {
            val (user, token) = dataGenerator.generateUser()
            repeat(3) { i ->
                createEventInDb(createdBy = user.id!!, title = "이벤트 $i")
                Thread.sleep(10) // createdAt 순서 보장
            }

            // 첫 페이지 (size=2) → 최신 2개
            val firstResult =
                mvc
                    .perform(
                        get("/api/events/me")
                            .header("Authorization", "Bearer $token")
                            .param("size", "2"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.events.length()").value(2))
                    .andExpect(jsonPath("$.hasNext").value(true))
                    .andReturn()

            val nextCursor = mapper.readTree(firstResult.response.contentAsString).get("nextCursor").asText()

            // 두 번째 페이지 (cursor 사용) → 나머지 1개
            mvc
                .perform(
                    get("/api/events/me")
                        .header("Authorization", "Bearer $token")
                        .param("size", "2")
                        .param("cursor", nextCursor),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
        }

        // =================================================================
        // PUT /api/events/{publicId} — 이벤트 수정
        // =================================================================

        @Test
        fun `주최자가 유효한 데이터로 이벤트 수정 시 200과 수정된 정보를 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "수정된 제목", location = "새 장소")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.location").value("새 장소"))
        }

        @Test
        fun `인증 없이 이벤트 수정 요청 시 401을 반환한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "수정")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `존재하지 않는 이벤트 수정 요청 시 404를 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    put("/api/events/${UUID.randomUUID()}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "수정")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
        }

        @Test
        fun `이벤트 주최자가 아닌 사용자가 수정 요청 시 403을 반환한다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (_, otherToken) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $otherToken")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "수정")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("NOT_EVENT_ADMIN"))
        }

        @Test
        fun `이벤트 제목을 공백으로 수정 요청 시 400을 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "   ")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_TITLE_BLANK"))
        }

        @Test
        fun `정원을 0으로 수정 요청 시 400을 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(capacity = 0)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_INVALID"))
        }

        @Test
        fun `정원을 음수로 수정 요청 시 400을 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(capacity = -1)))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_INVALID"))
        }

        @Test
        fun `모임 시작 시간을 과거로 수정 요청 시 400을 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(startsAt = Instant.now().minusSeconds(3600))))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("EVENT_STARTS_IN_PAST"))
        }

        @Test
        fun `수정 시 null 필드는 기존 값을 유지한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!, title = "원래 제목")

            // title만 수정, description은 null → 기존 description(null) 유지
            mvc
                .perform(
                    put("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token")
                        .content(mapper.writeValueAsString(UpdateEventRequest(title = "바뀐 제목")))
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.waitlistEnabled").value(false)) // 기존 값 유지
        }

        // =================================================================
        // DELETE /api/events/{publicId} — 이벤트 삭제
        // =================================================================

        @Test
        fun `확정된 참가자가 없는 이벤트를 주최자가 삭제 시 204를 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    delete("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isNoContent)
        }

        @Test
        fun `이벤트 삭제 후 해당 이벤트 조회 시 404를 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    delete("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isNoContent)

            mvc
                .perform(
                    get("/api/events/${event.publicId}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
        }

        @Test
        fun `인증 없이 이벤트 삭제 요청 시 401을 반환한다`() {
            val (user, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            mvc
                .perform(
                    delete("/api/events/${event.publicId}"),
                ).andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        }

        @Test
        fun `존재하지 않는 이벤트 삭제 요청 시 404를 반환한다`() {
            val (_, token) = dataGenerator.generateUser()

            mvc
                .perform(
                    delete("/api/events/${UUID.randomUUID()}")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
        }

        @Test
        fun `이벤트 주최자가 아닌 사용자가 삭제 요청 시 403을 반환한다`() {
            val (creator, _) = dataGenerator.generateUser()
            val (_, otherToken) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = creator.id!!)

            mvc
                .perform(
                    delete("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $otherToken"),
                ).andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("NOT_EVENT_ADMIN"))
        }

        @Test
        fun `확정된 참가자가 있는 이벤트 삭제 요청 시 409를 반환한다`() {
            val (user, token) = dataGenerator.generateUser()
            val (participant, _) = dataGenerator.generateUser()
            val event = createEventInDb(createdBy = user.id!!)

            registrationRepository.save(
                Registration(
                    userId = participant.id!!,
                    eventId = event.id!!,
                    status = RegistrationStatus.CONFIRMED,
                ),
            )

            mvc
                .perform(
                    delete("/api/events/${event.publicId}")
                        .header("Authorization", "Bearer $token"),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("EVENT_HAS_CONFIRMED_REGISTRATIONS"))
        }
    }

// =============================================================================
// [구체화 필요 사항]
//
// 1. CreateEventRequest.capacity 필드 null 허용 여부:
//    - DTO에서 capacity는 Int (non-nullable, 기본값 없음)로 선언되어 있어,
//      JSON 역직렬화 시 누락되면 Jackson이 400 에러를 반환함.
//    - 서비스의 validateCreateOrUpdate에는 EVENT_CAPACITY_REQUIRED(capacity==null) 검사가 있으나
//      CreateEventRequest에서는 실제로 null이 올 수 없으므로, 이 분기는 업데이트 시에만 의미 있음.
//    - 정책 확인 필요: capacity 생략 시 명시적인 400 + EVENT_CAPACITY_REQUIRED 응답이 필요하다면
//      DTO를 Int?로 변경하거나, @JsonSetter(nulls = Nulls.FAIL) 등 추가 처리가 필요.
//
// 2. waitlistEnabled 필드 null 허용 여부:
//    - CreateEventRequest.waitlistEnabled 역시 Boolean (non-nullable)로 선언되어,
//      생략 시 Jackson 역직렬화 단계에서 400이 발생함.
//    - 이것이 의도된 동작인지, 기본값(false)을 사용할 수 있도록 허용할지 명확화 필요.
//
// 3. 이벤트 수정 시 시간 범위 교차 검증:
//    - update는 필드를 개별적으로 덮어쓴 후 validateCreateOrUpdate를 호출함.
//    - 기존 이벤트의 startsAt=미래, 수정 요청에 startsAt만 더 미래로 변경하면
//      기존 registrationEndsAt이 새 startsAt보다 여전히 이전인지 재검증됨.
//    - 의도한 동작인지, 개별 필드 변경의 부작용으로 기존에 유효했던 조합이
//      무효화되는 케이스에 대한 정책 명확화 필요.
//
// 4. GET /api/events/{publicId} 에서 비인증 요청 시 viewer.name이 null인지 확인:
//    - requesterId가 null이면 viewerName = null로 설정되므로 viewer.name은 null이어야 함.
//    - UserArgumentResolver가 Authorization 헤더 없을 때 null을 반환하는지 확인 필요.
// =============================================================================
