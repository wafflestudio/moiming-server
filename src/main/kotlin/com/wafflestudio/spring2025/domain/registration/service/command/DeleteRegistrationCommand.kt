package com.wafflestudio.spring2025.domain.registration.service.command

sealed class DeleteRegistrationCommand {
    data class Member(
        val userId: Long,
        val registrationPublicId: String,
    ) : DeleteRegistrationCommand()

    data class Guest(
        val guestName: String,
        val guestEmail: String,
        val registrationPublicId: String,
    ) : DeleteRegistrationCommand()
}
