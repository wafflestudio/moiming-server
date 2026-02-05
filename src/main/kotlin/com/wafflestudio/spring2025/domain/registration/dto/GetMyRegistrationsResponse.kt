package com.wafflestudio.spring2025.domain.registration.dto

import com.wafflestudio.spring2025.domain.event.model.Event
import com.wafflestudio.spring2025.domain.registration.model.Registration
import com.wafflestudio.spring2025.domain.registration.model.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "내 이벤트 신청 목록 응답")
data class GetMyRegistrationsResponse(
    @Schema(description = "신청 목록")
    val registrations: List<MyRegistrationItem>,
)

@Schema(description = "내 이벤트 신청 요약")
data class MyRegistrationItem(
    @Schema(description = "이벤트 공개 ID")
    val publicId: String,
    @Schema(description = "이벤트 제목")
    val title: String,
    @Schema(description = "이벤트 시작 시간 (ISO-8601)")
    val startsAt: Instant?,
    @Schema(description = "이벤트 종료 시간 (ISO-8601)")
    val endsAt: Instant?,
    @Schema(description = "신청 시작 시간 (ISO-8601)")
    val registrationStartsAt: Instant?,
    @Schema(description = "신청 마감 시간 (ISO-8601)")
    val registrationEndsAt: Instant?,
    @Schema(description = "정원")
    val capacity: Int?,
    @Schema(description = "현재 신청 수 + 대기 수")
    val registrationCnt: Int,
    @Schema(description = "신청 상태")
    val status: MyRegistrationStatus,
    @Schema(description = "대기 순번 (WAITLISTED가 아니면 null)")
    val waitlistedNum: Int?,
) {
    constructor(
        registration: Registration,
        event: Event,
        registrationCnt: Int,
        waitlistedNum: Int?,
    ) : this(
        publicId = event.publicId,
        title = event.title,
        startsAt = event.startsAt,
        endsAt = event.endsAt,
        registrationStartsAt = event.registrationStartsAt,
        registrationEndsAt = event.registrationEndsAt,
        capacity = event.capacity,
        registrationCnt = registrationCnt,
        status = MyRegistrationStatus.from(registration.status),
        waitlistedNum = waitlistedNum,
    )
}

enum class MyRegistrationStatus {
    CONFIRMED,
    WAITLISTED,
    CANCELED,
    ;

    companion object {
        fun from(status: RegistrationStatus): MyRegistrationStatus =
            when (status) {
                RegistrationStatus.HOST,
                RegistrationStatus.CONFIRMED,
                -> CONFIRMED

                RegistrationStatus.WAITLISTED -> WAITLISTED

                RegistrationStatus.CANCELED,
                RegistrationStatus.BANNED,
                -> CANCELED
            }
    }
}
