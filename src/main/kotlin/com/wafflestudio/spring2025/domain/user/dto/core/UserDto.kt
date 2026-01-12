package com.wafflestudio.spring2025.domain.user.dto.core

import com.wafflestudio.spring2025.domain.user.model.User

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val profileImage: String?,
) {
    constructor(user: User) : this(
        id = TODO("User -> UserDto 매핑 구현"),
        email = TODO("User -> UserDto 매핑 구현"),
        name = TODO("User -> UserDto 매핑 구현"),
        profileImage = TODO("User -> UserDto 매핑 구현"),
    )
}
