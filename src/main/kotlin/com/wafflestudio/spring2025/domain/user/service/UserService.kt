package com.wafflestudio.spring2025.domain.user.service

import com.wafflestudio.spring2025.common.image.service.ImageService
import com.wafflestudio.spring2025.config.AwsS3Properties
import com.wafflestudio.spring2025.domain.auth.exception.AuthErrorCode
import com.wafflestudio.spring2025.domain.auth.exception.AuthValidationException
import com.wafflestudio.spring2025.domain.auth.exception.AuthenticationRequiredException
import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.exception.EmailChangeForbiddenException
import com.wafflestudio.spring2025.domain.user.exception.UserErrorCode
import com.wafflestudio.spring2025.domain.user.exception.UserException
import com.wafflestudio.spring2025.domain.user.exception.UserValidationException
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val s3Client: S3Client,
    private val s3Props: AwsS3Properties,
    private val imageService: ImageService,
) {
    fun me(user: User?): UserDto {
        if (user == null) throw AuthenticationRequiredException()

        val presignedUrl = user.profileImage?.let { imageService.presignedGetUrl(it) }

        return UserDto(
            id = user.id!!,
            email = user.email,
            name = user.name,
            profileImage = presignedUrl,
        )
    }

    fun patchMe(
        user: User,
        name: String?,
        email: String?,
        profileImage: String?,
        password: String?,
    ) {
        if (email != null) {
            throw EmailChangeForbiddenException()
        }
        name?.let { validateName(it) }
        password?.let { validatePassword(it) }
        profileImage?.let { validateProfileImage(it) }

        name?.let { user.name = it }
        password?.let { user.passwordHash = BCrypt.hashpw(it, BCrypt.gensalt()) }
        profileImage?.let { user.profileImage = it }

        userRepository.save(user)
    }

    fun deleteMe(
        user: User,
    ) {
        try {
            userRepository.delete(user)
        } catch (e: Exception) {
            throw UserException(UserErrorCode.NO_SUCH_USER)
        }

    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw AuthValidationException(AuthErrorCode.BAD_NAME)
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
        if (!password.any { it.isLetter() }) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
        if (!password.any { it.isDigit() }) {
            throw AuthValidationException(AuthErrorCode.BAD_PASSWORD)
        }
    }

    private fun validateProfileImage(profileImageKey: String) {
        try {
            s3Client.headObject(
                HeadObjectRequest
                    .builder()
                    .bucket(s3Props.bucket)
                    .key(profileImageKey)
                    .build(),
            )
        } catch (_: NoSuchKeyException) {
            throw UserValidationException(UserErrorCode.PROFILE_IMAGE_NOT_FOUND)
        }
    }
}
