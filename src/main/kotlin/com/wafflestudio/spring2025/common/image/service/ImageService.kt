package com.wafflestudio.spring2025.common.image.service

import com.wafflestudio.spring2025.common.image.dto.ImageUploadResponse
import com.wafflestudio.spring2025.common.image.exception.ImageErrorCode
import com.wafflestudio.spring2025.common.image.exception.ImageValidationException
import com.wafflestudio.spring2025.config.AwsS3Properties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
class ImageService(
    private val s3Client: S3Client,
    private val presigner: S3Presigner,
    private val s3Props: AwsS3Properties,
) {
    fun uploadImage(
        ownerId: Long?,
        image: MultipartFile,
        prefix: String?,
    ): ImageUploadResponse {
        validateImage(image)
        validatePrefix(prefix)

        val directory = sanitizePrefix(prefix)
        val ext = extractExtension(image.originalFilename)
        val key = listOf(directory, ownerId?.toString() ?: "new_user", "${UUID.randomUUID()}$ext").joinToString(separator = "/")

        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(s3Props.bucket)
                .key(key)
                .contentType(image.contentType ?: "application/octet-stream")
                .build()

        image.inputStream.use { inputStream ->
            s3Client.putObject(
                putRequest,
                RequestBody.fromInputStream(inputStream, image.size),
            )
        }

        val url = presignedGetUrl(key)
        return ImageUploadResponse(key = key, url = url)
    }

    private fun validateImage(image: MultipartFile) {
        if (image.isEmpty) {
            throw ImageValidationException(ImageErrorCode.IMAGE_FILE_EMPTY)
        }
        val contentType = image.contentType ?: ""
        if (!contentType.startsWith("image/")) {
            throw ImageValidationException(ImageErrorCode.IMAGE_FILE_TYPE_INVALID)
        }
        if (image.size > MAX_IMAGE_BYTES) {
            throw ImageValidationException(ImageErrorCode.IMAGE_FILE_TOO_LARGE)
        }
    }

    private fun validatePrefix(prefix: String?) {
        val trimmed = prefix?.trim()?.trim('/') ?: return
        if (trimmed !in ALLOWED_PREFIXES) {
            throw ImageValidationException(ImageErrorCode.IMAGE_PREFIX_NOT_ALLOWED)
        }
    }

    private fun sanitizePrefix(prefix: String?): String {
        val trimmed = prefix?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_PREFIX
        return trimmed.trim('/').ifEmpty { DEFAULT_PREFIX }
    }

    private fun extractExtension(originalFilename: String?): String {
        if (originalFilename.isNullOrBlank()) return DEFAULT_EXTENSION
        val lastDot = originalFilename.lastIndexOf('.')
        if (lastDot == -1) return DEFAULT_EXTENSION
        val ext = originalFilename.substring(lastDot).lowercase()
        return if (ext in ALLOWED_EXTENSIONS) ext else DEFAULT_EXTENSION
    }

    fun deleteImage(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(s3Props.bucket)
                .key(key)
                .build(),
        )
    }

    fun presignedGetUrl(key: String): String {
        val getRequest =
            GetObjectRequest
                .builder()
                .bucket(s3Props.bucket)
                .key(key)
                .build()

        val presignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(s3Props.presignExpireSeconds))
                .getObjectRequest(getRequest)
                .build()

        return presigner.presignGetObject(presignRequest).url().toString()
    }

    companion object {
        private const val DEFAULT_PREFIX = "images"
        private const val DEFAULT_EXTENSION = ".jpg"
        private const val MAX_IMAGE_BYTES = 5L * 1024 * 1024
        private val ALLOWED_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".webp")
        private val ALLOWED_PREFIXES = setOf("profile-images")
    }
}
