package com.wafflestudio.spring2025

import com.wafflestudio.spring2025.common.image.service.ImageService
import com.wafflestudio.spring2025.domain.event.exception.EventErrorCode
import com.wafflestudio.spring2025.domain.event.exception.EventValidationException
import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.event.repository.EventLockRepository
import com.wafflestudio.spring2025.domain.event.repository.EventRepository
import com.wafflestudio.spring2025.domain.event.service.EventService
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import com.wafflestudio.spring2025.domain.registration.repository.RegistrationRepository
import com.wafflestudio.spring2025.domain.registration.service.WaitlistReconciliationService
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.UUID

class EventServiceTest {
    private val eventRepository = mock(EventRepository::class.java)
    private val eventLockRepository = mock(EventLockRepository::class.java)
    private val registrationRepository = mock(RegistrationRepository::class.java)
    private val waitlistReconciliationService = mock(WaitlistReconciliationService::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val imageService = mock(ImageService::class.java)

    private val eventService =
        EventService(
            eventRepository = eventRepository,
            eventLockRepository = eventLockRepository,
            registrationRepository = registrationRepository,
            waitlistReconciliationService = waitlistReconciliationService,
            userRepository = userRepository,
            imageService = imageService,
        )

    @Test
    fun `registration start cannot be delayed when confirmed exists`() {
        val event = makeEvent()
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 1, waitlisted = 0)

        val exception =
            assertThrows<EventValidationException> {
                eventService.update(
                    publicId = event.publicId,
                    title = null,
                    description = null,
                    location = null,
                    startsAt = null,
                    endsAt = null,
                    capacity = null,
                    waitlistEnabled = null,
                    registrationStartsAt = event.registrationStartsAt!!.plusSeconds(300),
                    registrationEndsAt = null,
                    requesterId = event.createdBy,
                )
            }

        assertThat(exception.code).isEqualTo(EventErrorCode.REGISTRATION_START_CANNOT_DELAY_WITH_PARTICIPANTS)
    }

    @Test
    fun `registration end cannot be advanced when confirmed exists`() {
        val event = makeEvent()
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 1, waitlisted = 0)

        val exception =
            assertThrows<EventValidationException> {
                eventService.update(
                    publicId = event.publicId,
                    title = null,
                    description = null,
                    location = null,
                    startsAt = null,
                    endsAt = null,
                    capacity = null,
                    waitlistEnabled = null,
                    registrationStartsAt = null,
                    registrationEndsAt = event.registrationEndsAt!!.minusSeconds(300),
                    requesterId = event.createdBy,
                )
            }

        assertThat(exception.code).isEqualTo(EventErrorCode.REGISTRATION_END_CANNOT_ADVANCE_WITH_PARTICIPANTS)
    }

    @Test
    fun `registration window can be changed when only waitlisted exists`() {
        val event = makeEvent(capacity = 10)
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 0, waitlisted = 1)
        val expectedStart = event.registrationStartsAt!!.plusSeconds(300)
        val expectedEnd = event.registrationEndsAt!!.minusSeconds(300)

        val updated =
            eventService.update(
                publicId = event.publicId,
                title = null,
                description = null,
                location = null,
                startsAt = null,
                endsAt = null,
                capacity = null,
                waitlistEnabled = null,
                registrationStartsAt = expectedStart,
                registrationEndsAt = expectedEnd,
                requesterId = event.createdBy,
            )

        assertThat(updated.registrationStartsAt).isEqualTo(expectedStart)
        assertThat(updated.registrationEndsAt).isEqualTo(expectedEnd)
    }

    @Test
    fun `capacity cannot be set below confirmed count`() {
        val event = makeEvent(capacity = 10)
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 3, waitlisted = 5)

        val exception =
            assertThrows<EventValidationException> {
                eventService.update(
                    publicId = event.publicId,
                    title = null,
                    description = null,
                    location = null,
                    startsAt = null,
                    endsAt = null,
                    capacity = 2,
                    waitlistEnabled = null,
                    registrationStartsAt = null,
                    registrationEndsAt = null,
                    requesterId = event.createdBy,
                )
            }

        assertThat(exception.code).isEqualTo(EventErrorCode.CAPACITY_CANNOT_DECREASE_WITH_PARTICIPANTS)
    }

    @Test
    fun `capacity can be lowered to at least confirmed count`() {
        val event = makeEvent(capacity = 10)
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 3, waitlisted = 10)

        val updated =
            eventService.update(
                publicId = event.publicId,
                title = null,
                description = null,
                location = null,
                startsAt = null,
                endsAt = null,
                capacity = 3,
                waitlistEnabled = null,
                registrationStartsAt = null,
                registrationEndsAt = null,
                requesterId = event.createdBy,
            )

        assertThat(updated.capacity).isEqualTo(3)
        verify(waitlistReconciliationService, never()).reconcileWaitlist(event.id!!)
    }

    @Test
    fun `increasing capacity delegates waitlist reconciliation`() {
        val event = makeEvent(capacity = 1)
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 1, waitlisted = 2)

        val updated =
            eventService.update(
                publicId = event.publicId,
                title = null,
                description = null,
                location = null,
                startsAt = null,
                endsAt = null,
                capacity = 2,
                waitlistEnabled = null,
                registrationStartsAt = null,
                registrationEndsAt = null,
                requesterId = event.createdBy,
            )

        assertThat(updated.capacity).isEqualTo(2)
        verify(waitlistReconciliationService).reconcileWaitlist(event.id!!)
    }

    @Test
    fun `both registration start and capacity can be changed when no confirmed exists`() {
        val event = makeEvent(capacity = 10)
        stubEventLookup(event)
        stubParticipantCounts(eventId = event.id!!, confirmed = 0, waitlisted = 0)
        val expectedRegistrationStartsAt = event.registrationStartsAt!!.plusSeconds(300)

        val updated =
            eventService.update(
                publicId = event.publicId,
                title = null,
                description = null,
                location = null,
                startsAt = null,
                endsAt = null,
                capacity = 5,
                waitlistEnabled = null,
                registrationStartsAt = expectedRegistrationStartsAt,
                registrationEndsAt = null,
                requesterId = event.createdBy,
            )

        assertThat(updated.capacity).isEqualTo(5)
        assertThat(updated.registrationStartsAt).isEqualTo(expectedRegistrationStartsAt)
        verify(waitlistReconciliationService, never()).reconcileWaitlist(event.id!!)
    }

    private fun stubEventLookup(event: Event) {
        `when`(eventRepository.findByPublicId(event.publicId)).thenReturn(event)
        `when`(eventRepository.save(any(Event::class.java))).thenAnswer { it.arguments[0] as Event }
    }

    private fun stubParticipantCounts(
        eventId: Long,
        confirmed: Long,
        waitlisted: Long,
    ) {
        `when`(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)).thenReturn(confirmed)
        `when`(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(waitlisted)
    }

    private fun makeEvent(capacity: Int = 10): Event {
        val now = Instant.now().plusSeconds(3600)
        return Event(
            id = 1L,
            publicId = UUID.randomUUID().toString(),
            title = "title",
            description = "description",
            location = "location",
            startsAt = now.plusSeconds(3600),
            endsAt = now.plusSeconds(7200),
            capacity = capacity,
            waitlistEnabled = true,
            registrationStartsAt = now.plusSeconds(300),
            registrationEndsAt = now.plusSeconds(1200),
            createdBy = 100L,
            createdAt = now,
            updatedAt = now,
        )
    }
}
