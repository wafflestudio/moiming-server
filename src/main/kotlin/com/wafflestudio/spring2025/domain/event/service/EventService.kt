package com.wafflestudio.spring2025.domain.event.service

import com.wafflestudio.spring2025.common.image.service.ImageService
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
    private val imageService: ImageService,
) {
    /**
     * 일정 생성
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

        return eventRepository.save(event).publicId
    }

    /**
     * 일정 상세 조회
     */
    fun getDetail(
        publicId: String,
        requesterId: Long?,
    ): EventDetailResponse {
        val event = getEventByPublicId(publicId)
        val eventId = requireNotNull(event.id) { "Event id is null: publicId=$publicId" }

        val creatorUser =
            userRepository.findById(event.createdBy).orElseThrow {
                EventNotFoundException()
            }

        val myReg =
            if (requesterId == null) {
                null
            } else {
                registrationRepository.findByUserIdAndEventId(
                    userId = requesterId,
                    eventId = eventId,
                )
            }

        val confirmedCount =
            registrationRepository
                .countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.CONFIRMED)
                .toInt()

        val waitlistedCount =
            registrationRepository
                .countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.WAITLISTED)
                .toInt()

        val totalApplicants = confirmedCount + waitlistedCount

        val waitlistPosition: Int? =
            if (myReg?.status == RegistrationStatus.WAITLISTED) {
                val waitlistedRegs =
                    registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                        eventID = eventId,
                        registrationStatus = RegistrationStatus.WAITLISTED,
                    )
                val idx = waitlistedRegs.indexOfFirst { it.id == myReg.id }
                if (idx >= 0) idx + 1 else null
            } else {
                null
            }

        val viewerStatus: ViewerStatus =
            when {
                requesterId == null -> ViewerStatus.NONE
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

        val viewerName: String? =
            requesterId?.let {
                userRepository.findById(it).orElse(null)?.name
            }

        val viewer =
            if (viewerStatus == ViewerStatus.NONE) {
                ViewerInfo(
                    status = ViewerStatus.NONE,
                    name = viewerName,
                    waitlistPosition = null,
                    registrationPublicId = null,
                    reservationEmail = null,
                )
            } else {
                ViewerInfo(
                    status = viewerStatus,
                    name = viewerName,
                    waitlistPosition = waitlistPosition,
                    registrationPublicId = myReg?.registrationPublicId,
                    reservationEmail = myReg?.guestEmail,
                )
            }

        val capabilities =
            buildCapabilities(
                viewerStatus = viewerStatus,
                capacity = event.capacity,
                confirmedCount = confirmedCount,
                waitlistEnabled = event.waitlistEnabled,
                registrationStartsAt = event.registrationStartsAt,
                registrationEndsAt = event.registrationEndsAt,
            )

        val confirmedRegs =
            registrationRepository.findByEventIdAndStatusOrderByCreatedAtAsc(
                eventID = eventId,
                registrationStatus = RegistrationStatus.CONFIRMED,
            )

        val previewUserIds =
            confirmedRegs.mapNotNull { it.userId }.distinct().take(5)

        val usersById =
            userRepository.findAllById(previewUserIds).associateBy { it.id!! }

        val guestsPreview =
            previewUserIds.mapNotNull { uid ->
                usersById[uid]?.let {
                    GuestPreview(
                        id = it.id!!,
                        name = it.name,
                        profileImage = it.profileImage?.let { key -> imageService.presignedGetUrl(key) },
                    )
                }
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
                    profileImage = creatorUser.profileImage?.let { imageService.presignedGetUrl(it) },
                ),
            viewer = viewer,
            capabilities = capabilities,
            guestsPreview = guestsPreview,
        )
    }

    /**
     * 내가 만든 일정 조회 (무한 스크롤)
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
                pageSize + 1,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        val fetched =
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
                val eventId = requireNotNull(event.id)

                val confirmedCount =
                    registrationRepository
                        .countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.CONFIRMED)
                        .toInt()

                val waitlistedCount =
                    registrationRepository
                        .countByEventIdAndStatus(eventID = eventId, registrationStatus = RegistrationStatus.WAITLISTED)
                        .toInt()

                MyEventResponse(
                    publicId = event.publicId,
                    title = event.title,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                    registrationStartsAt = event.registrationStartsAt,
                    registrationEndsAt = event.registrationEndsAt,
                    capacity = event.capacity,
                    totalApplicants = confirmedCount + waitlistedCount,
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
        requireCreator(event, requesterId)

        title?.let {
            if (it.isBlank()) throw EventValidationException(EventErrorCode.EVENT_TITLE_BLANK)
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
        requireCreator(event, requesterId)
        eventRepository.deleteById(requireNotNull(event.id))
    }

    private fun getEventByPublicId(publicId: String): Event =
        eventRepository.findByPublicId(publicId)
            ?: throw EventNotFoundException()

    private fun requireCreator(
        event: Event,
        requesterId: Long,
    ) {
        if (event.createdBy != requesterId) {
            throw EventForbiddenException()
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

        if (registrationStartsAt != null &&
            registrationEndsAt != null &&
            registrationStartsAt.isAfter(registrationEndsAt)
        ) {
            throw EventValidationException(EventErrorCode.REGISTRATION_ENDS_BEFORE_STARTS)
        }

        if (registrationEndsAt != null &&
            startsAt != null &&
            registrationEndsAt.isAfter(startsAt)
        ) {
            throw EventValidationException(EventErrorCode.REGISTRATION_ENDS_AFTER_EVENT_START)
        }

        if (registrationStartsAt != null &&
            startsAt != null &&
            registrationStartsAt.isAfter(startsAt)
        ) {
            throw EventValidationException(EventErrorCode.REGISTRATION_STARTS_AFTER_EVENT_START)
        }
    }

    private fun buildCapabilities(
        viewerStatus: ViewerStatus,
        capacity: Int?,
        confirmedCount: Int,
        waitlistEnabled: Boolean,
        registrationStartsAt: Instant?,
        registrationEndsAt: Instant?,
        now: Instant = Instant.now(),
    ): CapabilitiesInfo {
        val withinWindow =
            (registrationStartsAt?.let { !now.isBefore(it) } ?: true) &&
                (registrationEndsAt?.let { !now.isAfter(it) } ?: true)

        val isFull =
            capacity != null && confirmedCount >= capacity

        // 확정 자리 신청 가능
        val canApply = withinWindow && !isFull

        // 대기 신청 가능: 정원 찼고 + 대기 허용
        val canWait = withinWindow && isFull && waitlistEnabled

        return when (viewerStatus) {
            ViewerStatus.HOST ->
                CapabilitiesInfo(
                    shareLink = true,
                    apply = false,
                    wait = false,
                    cancel = false,
                )

            ViewerStatus.CONFIRMED, ViewerStatus.WAITLISTED ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = false,
                    wait = false,
                    cancel = true,
                )

            ViewerStatus.CANCELED, ViewerStatus.NONE ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = canApply,
                    wait = canWait,
                    cancel = false,
                )

            ViewerStatus.BANNED ->
                CapabilitiesInfo(
                    shareLink = false,
                    apply = false,
                    wait = false,
                    cancel = false,
                )
        }
    }
}
