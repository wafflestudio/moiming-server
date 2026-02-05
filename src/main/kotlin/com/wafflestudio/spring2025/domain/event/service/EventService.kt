package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.domain.event.dto.response.CapabilitiesInfo
import com.wafflestudio.spring2025.domain.event.dto.response.CreatorInfo
import com.wafflestudio.spring2025.domain.event.dto.response.EventDetailResponse
import com.wafflestudio.spring2025.domain.event.dto.response.EventInfo
import com.wafflestudio.spring2025.domain.event.dto.response.GuestPreview
import com.wafflestudio.spring2025.domain.event.dto.response.MyEventResponse
import com.wafflestudio.spring2025.domain.event.dto.response.MyEventsInfiniteResponse
import com.wafflestudio.spring2025.domain.event.dto.response.ViewerInfo
import com.wafflestudio.spring2025.domain.event.dto.response.ViewerStatus
import com.wafflestudio.spring2025.domain.event.exception.EventErrorCode
import com.wafflestudio.spring2025.domain.event.exception.EventForbiddenException
import com.wafflestudio.spring2025.domain.event.exception.EventNotFoundException
import com.wafflestudio.spring2025.domain.event.exception.EventValidationException
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val userRepository: UserRepository,
) {
    /**
     * 일정 생성
     * API 설계상 body를 비우고 201 + Location을 주기 위해 생성된 publicId를 반환
     */
    fun create(
        title: String,
        description: String?,
        location: String?,
        startsAt: Instant?,
        endsAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
        createdBy: Long,
    ): String {
        validateCreateOrUpdate(
            title = title,
            startsAt = startsAt,
            endsAt = endsAt,
            capacity = capacity,
            registrationStartsAt = registrationStartsAt,
            registrationEndsAt = registrationEndsAt,
        )

        val event =
            Event(
                publicId = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description,
                location = location,
                startsAt = startsAt,
                endsAt = endsAt,
                capacity = capacity,
                waitlistEnabled = waitlistEnabled,
                registrationStartsAt = registrationStartsAt,
                registrationEndsAt = registrationEndsAt,
                createdBy = createdBy,
            )

        val saved = eventRepository.save(event)
        return saved.publicId
    }

    /**
     * 일정 상세 조회
     */
    fun getDetail(
        publicId: String,
        requesterId: Long,
    ): EventDetailResponse {
        val event = getEventByPublicId(publicId)

        val eventId =
            requireNotNull(event.id) {
                "Event internal id is null: publicId=$publicId"
            }

        // creator 조회
        val creatorUser =
            userRepository.findById(event.createdBy).orElseThrow {
                // createdBy가 users FK면 거의 안 나지만, 안전하게
                EventNotFoundException(publicId)
            }

        // viewer(로그인 유저)의 registration
        val myReg =
            registrationRepository.findByUserIdAndEventId(
                userId = requesterId,
                eventId = eventId,
            )

        // totalApplicants = HOST + CONFIRMED + WAITING (CANCELED/BANNED 제외)
        val hostCnt = 1
        val confirmedCnt =
            registrationRepository.countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.CONFIRMED).toInt()
        val waitingCnt =
            registrationRepository.countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.WAITING).toInt()
        val totalApplicants = hostCnt + confirmedCnt + waitingCnt

        // waitlistPosition: 내 상태가 WAITING일 때만 계산
        val waitlistPosition: Int? =
            if (myReg?.status == RegistrationStatus.WAITING) {
                val waitings =
                    registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                        eventID = eventId,
                        registrationStatus = RegistrationStatus.WAITING,
                    )
                val idx = waitings.indexOfFirst { it.id == myReg.id }
                if (idx >= 0) idx + 1 else null
            } else {
                null
            }

        // viewer.status 결정
        val viewerStatus: ViewerStatus =
            when {
                requesterId == event.createdBy -> ViewerStatus.HOST
                myReg == null -> ViewerStatus.NONE
                else ->
                    when (myReg.status) {
                        RegistrationStatus.HOST -> ViewerStatus.HOST
                        RegistrationStatus.CONFIRMED -> ViewerStatus.CONFIRMED
                        RegistrationStatus.WAITLISTED -> ViewerStatus.WAITLISTED
                        RegistrationStatus.CANCELED -> ViewerStatus.CANCELED
                        RegistrationStatus.BANNED -> ViewerStatus.BANNED
                    }
            }

        val isGuest = myReg?.userId == null && myReg != null
        val viewer =
            ViewerInfo(
                status = viewerStatus,
                waitlistPosition = waitlistPosition,
                registrationPublicId = if (isGuest) myReg!!.registrationPublicId else null,
                reservationEmail = if (isGuest) myReg!!.guestEmail else null,
            )

        // capabilities
        val capabilities =
            buildCapabilities(
                viewerStatus = viewerStatus,
                capacity = event.capacity,
                hostCount = hostCnt,
                confirmedCount = confirmedCnt,
                waitlistEnabled = event.waitlistEnabled,
                registrationStartsAt = event.registrationStartsAt,
                registrationEndsAt = event.registrationEndsAt,
            )

        // guestsPreview: CONFIRMED 중 userId 있는 회원만 최대 5명
        val confirmedRegs =
            registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventID = eventId,
                registrationStatus = RegistrationStatus.CONFIRMED,
            )

        val previewUserIds =
            confirmedRegs.mapNotNull { it.userId }.distinct().take(5)

        val usersById =
            userRepository.findAllById(previewUserIds).associateBy { it.id!! }

        val guestsPreview: List<GuestPreview> =
            previewUserIds.mapNotNull { uid ->
                val u = usersById[uid] ?: return@mapNotNull null
                GuestPreview(
                    id = u.id!!,
                    name = u.name,
                    profileImage = u.profileImage,
                )
            }

        return EventDetailResponse(
            event =
                EventInfo(
                    publicId = event.publicId,
                    title = event.title,
                    description = event.description,
                    location = event.location,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                    capacity = event.capacity,
                    totalApplicants = totalApplicants,
                    registrationStartsAt = event.registrationStartsAt,
                    registrationEndsAt = event.registrationEndsAt,
                ),
            creator =
                CreatorInfo(
                    name = creatorUser.name,
                    email = creatorUser.email,
                    profileImage = creatorUser.profileImage,
                ),
            viewer = viewer,
            capabilities = capabilities,
            guestsPreview = guestsPreview,
        )
    }

    /**
     * 내가 만든 일정 조회
     * 처음에는 cursor가 null, 그 이후에는 이전 Response에서 받은 cursor로 목록 조회
     */
    fun getMyEventsInfinite(
        createdBy: Long,
        cursor: Instant?,
        size: Int,
    ): MyEventsInfiniteResponse {
        val pageSize = size.coerceAtLeast(1)
        val pageable =
            PageRequest.of(
                0,
                pageSize + 1, // +1로 hasNext 판단
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        val fetched: List<Event> =
            if (cursor == null) {
                eventRepository.findByCreatedByAndCreatedAtIsNotNullOrderByCreatedAtDesc(createdBy, pageable)
            } else {
                eventRepository.findByCreatedByAndCreatedAtIsNotNullAndCreatedAtLessThanOrderByCreatedAtDesc(
                    createdBy = createdBy,
                    cursor = cursor,
                    pageable = pageable,
                )
            }

        val hasNext = fetched.size > pageSize
        val sliced = fetched.take(pageSize)

        val nextCursor = sliced.lastOrNull()?.createdAt

        val responses =
            sliced.map { event ->
                val eventId = requireNotNull(event.id) { "Event id is null: publicId=${event.publicId}" }

                val hostCnt = 1

                val confirmedCnt =
                    registrationRepository
                        .countByEventIdAndStatus(
                            eventID = eventId,
                            registrationStatus = RegistrationStatus.CONFIRMED,
                        ).toInt()

                val waitingCnt =
                    registrationRepository
                        .countByEventIdAndStatus(
                            eventID = eventId,
                            registrationStatus = RegistrationStatus.WAITING,
                        ).toInt()

                MyEventResponse(
                    publicId = event.publicId,
                    title = event.title,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                    registrationStartsAt = event.registrationStartsAt,
                    registrationEndsAt = event.registrationEndsAt,
                    capacity = event.capacity,
                    totalApplicants = hostCnt + confirmedCnt + waitingCnt,
                )
            }

        return MyEventsInfiniteResponse(
            events = responses,
            nextCursor = if (hasNext) nextCursor else null,
            hasNext = hasNext,
        )
    }

    fun update(
        publicId: String,
        title: String?,
        description: String?,
        location: String?,
        startsAt: Instant?,
        endsAt: Instant?,
        capacity: Int?,
        waitlistEnabled: Boolean?,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
        requesterId: Long,
    ): Event {
        val event = getEventByPublicId(publicId)

        // 생성자 권한 체크
        requireCreator(event, requesterId)

        // null은 "변경 없음"
        title?.let {
            if (it.isBlank()) {
                throw EventValidationException(EventErrorCode.EVENT_TITLE_BLANK)
            }
            event.title = it.trim()
        }
        description?.let { event.description = it }
        location?.let { event.location = it }
        startsAt?.let { event.startsAt = it }
        endsAt?.let { event.endsAt = it }
        capacity?.let { event.capacity = it }
        waitlistEnabled?.let { event.waitlistEnabled = it }
        registrationStartsAt?.let { event.registrationStartsAt = it }
        registrationEndsAt?.let { event.registrationEndsAt = it }

        // 변경 후에도 도메인 규칙 검증
        validateCreateOrUpdate(
            title = event.title,
            startsAt = event.startsAt,
            endsAt = event.endsAt,
            capacity = event.capacity,
            registrationStartsAt = event.registrationStartsAt,
            registrationEndsAt = event.registrationEndsAt,
        )

        return eventRepository.save(event)
    }

    fun delete(
        publicId: String,
        requesterId: Long,
    ) {
        val event = getEventByPublicId(publicId)

        // 생성자 권한 체크
        requireCreator(event, requesterId)

        // 내부 PK로 삭제
        eventRepository.deleteById(requireNotNull(event.id))
    }

    private fun getEventByPublicId(publicId: String): Event =
        eventRepository.findByPublicId(publicId)
            ?: throw EventNotFoundException(publicId)

    private fun requireCreator(
        event: Event,
        requesterId: Long,
    ) {
        if (event.createdBy != requesterId) {
            throw EventForbiddenException(requesterId)
        }
    }

    private fun validateCreateOrUpdate(
        title: String,
        startsAt: Instant?,
        endsAt: Instant?,
        capacity: Int?,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
    ) {
        if (title.isBlank()) {
            throw EventValidationException(EventErrorCode.EVENT_TITLE_BLANK)
        }

        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw EventValidationException(EventErrorCode.EVENT_TIME_RANGE_INVALID)
        }

        if (capacity != null && capacity <= 0) {
            throw EventValidationException(EventErrorCode.EVENT_CAPACITY_INVALID)
        }

        // registrationStartsAt / registrationEndsAt 관계
        if (registrationStartsAt != null &&
            registrationEndsAt != null &&
            registrationStartsAt.isAfter(registrationEndsAt)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }

        // registrationEndsAt <= startsAt
        if (registrationEndsAt != null &&
            startsAt != null &&
            registrationEndsAt.isAfter(startsAt)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }

        // registrationStartsAt <= startsAt
        if (registrationStartsAt != null &&
            startsAt != null &&
            registrationStartsAt.isAfter(startsAt)
        ) {
            throw EventValidationException(EventErrorCode.EVENT_REGISTRATION_WINDOW_INVALID)
        }
    }

    private fun buildCapabilities(
        viewerStatus: ViewerStatus,
        capacity: Int?,
        hostCount: Int,
        confirmedCount: Int,
        waitlistEnabled: Boolean,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
        now: Instant = Instant.now(),
    ): CapabilitiesInfo {
        // 신청 가능 시간(window) 판단
        val withinWindow =
            (registrationStartsAt?.let { !now.isBefore(it) } ?: true) &&
                    (registrationEndsAt?.let { !now.isAfter(it) } ?: true)

        // 정원 판단: HOST + CONFIRMED 기준
        val isFull =
            capacity != null && (hostCount + confirmedCount) >= capacity

        // 현재 신청 가능 조건
        val canApplyNow =
            withinWindow && (!isFull || waitlistEnabled)

        return when (viewerStatus) {
            ViewerStatus.HOST ->
                CapabilitiesInfo(
                    shareLink = true,
                    apply = false,
                    cancel = false,
                )

            ViewerStatus.CONFIRMED, ViewerStatus.WAITLISTED ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = false,
                    cancel = true,
                )

            ViewerStatus.CANCELLED, ViewerStatus.NONE ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = canApplyNow,
                    cancel = false,
                )

            ViewerStatus.BANNED ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = false,
                    cancel = false,
                )
        }
    }
}
