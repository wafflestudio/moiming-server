package com.wafflestudio.spring2025.domain.user.service

import com.wafflestudio.spring2025.config.AwsS3Properties
import com.wafflestudio.spring2025.domain.auth.exception.AuthenticationRequiredException
import com.wafflestudio.spring2025.domain.user.dto.core.UserDto
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Service
class UserService(
    private val userRepository: UserRepository,
    private val s3Client: S3Client,
    private val presigner: S3Presigner,
    private val s3Props: AwsS3Properties,
) {
    fun me(user: User?): UserDto {
        if (user == null) throw AuthenticationRequiredException()

        val presignedUrl = user.profileImage?.let { key -> presignedGetUrl(key) }

        return UserDto(
            id = user.id!!,
            email = user.email,
            name = user.name,
            profileImage = presignedUrl,
        )
    }

//
//    fun updateProfileImage(
//        user: User?,
//        image: MultipartFile,
//    ) {
//        if (user == null) throw AuthenticationRequiredException()
//        validateProfileImage(image)
//
//        val ext = extractExtension(image.originalFilename)
//        val key = "profile-images/${user.id}/${UUID.randomUUID()}$ext"
//
//        val putReq =
//            PutObjectRequest
//                .builder()
//                .bucket(s3Props.bucket)
//                .key(key)
//                .contentType(image.contentType ?: "application/octet-stream")
//                .build()
//
//        image.inputStream.use { input ->
//            s3Client.putObject(
//                putReq,
//                RequestBody.fromInputStream(input, image.size),
//            )
//        }
//
//        // DB에는 URL이 아니라 key 저장
//        user.profileImage = key
//        userRepository.save(user)
//    }
//
//    fun deleteProfileImage(user: User?) {
//        if (user == null) throw AuthenticationRequiredException()
//
//        // (선택) S3에서도 삭제
//        user.profileImage?.let { key ->
//            val delReq =
//                DeleteObjectRequest
//                    .builder()
//                    .bucket(s3Props.bucket)
//                    .key(key)
//                    .build()
//            runCatching { s3Client.deleteObject(delReq) }
//        }
//
//        user.profileImage = null
//        userRepository.save(user)
//    }
//
    private fun presignedGetUrl(key: String): String {
        val getReq =
            GetObjectRequest
                .builder()
                .bucket(s3Props.bucket)
                .key(key)
                .build()

        val presignReq =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(s3Props.presignExpireSeconds))
                .getObjectRequest(getReq)
                .build()

        return presigner.presignGetObject(presignReq).url().toString()
    }
//
//    private fun validateProfileImage(image: MultipartFile) {
//        if (image.isEmpty) throw UserValidationException(UserErrorCode.PROFILE_IMAGE_EMPTY)
//        val contentType = image.contentType ?: ""
//        if (!contentType.startsWith("image/")) throw UserValidationException(UserErrorCode.PROFILE_IMAGE_FORMAT_INVALID)
//
//        val maxSizeBytes = 5L * 1024L * 1024L
//        if (image.size > maxSizeBytes) throw UserValidationException(UserErrorCode.PROFILE_IMAGE_TOO_LARGE)
//    }
//
//    private fun extractExtension(originalFilename: String?): String {
//        if (originalFilename.isNullOrBlank()) return ".jpg"
//        val idx = originalFilename.lastIndexOf('.')
//        if (idx < 0) return ".jpg"
//        val ext = originalFilename.substring(idx).lowercase()
//        return if (ext in setOf(".jpg", ".jpeg", ".png", ".webp")) ext else ".jpg"
//    }
}
