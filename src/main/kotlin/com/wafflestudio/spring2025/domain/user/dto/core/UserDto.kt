package com.wafflestudio.spring2025.domain.user.dto.core

import com.wafflestudio.spring2025.domain.user.model.User

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val profileImage: String?,
) {
    constructor(user: User) : this(
        id = user.id!!,
        email = user.email,
        name = user.name,
        profileImage = user.profileImage,
    )
}
