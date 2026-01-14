package com.wafflestudio.spring2025.domain.user.service

import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.user.EmailAlreadyExistsException
import com.wafflestudio.spring2025.domain.user.InvalidVerificationCodeException
import com.wafflestudio.spring2025.domain.user.VerificationCodeExpiredException
import com.wafflestudio.spring2025.domain.user.model.PendingUser
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.PendingUserRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val pendingUserRepository: PendingUserRepository,
    private val emailService: EmailService,
) {
    companion object {
        private const val VERIFICATION_CODE_EXPIRATION_HOURS = 24L
    }

    /**
     * 대기 중인 사용자 등록을 생성하고 인증 이메일을 발송합니다
     * @param email 사용자 이메일 주소
     * @param name 사용자 이름
     * @param passwordHash 해시된 비밀번호
     * @throws EmailAlreadyExistsException 이메일이 users 또는 pending_users에 이미 존재하는 경우
     */
    fun createPendingUser(
        email: String,
        name: String,
        passwordHash: String,
    ) {
        // Check if email already exists in users or pending_users
        if (userRepository.existsByEmail(email) || pendingUserRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException()
        }

        // Generate verification code
        val verificationCode = generateVerificationCode()

        // Calculate expiration time
        val expiresAt = Instant.now().plusSeconds(VERIFICATION_CODE_EXPIRATION_HOURS * 3600)

        // Save pending user
        val pendingUser =
            PendingUser(
                email = email,
                name = name,
                passwordHash = passwordHash,
                verificationCode = verificationCode,
                expiresAt = expiresAt,
            )

        pendingUserRepository.save(pendingUser)

        // Send verification email
        emailService.sendVerificationEmail(email, verificationCode)
    }

    /**
     * Verifies the verification code and creates a verified user
     * @param code Verification code from email
     * @return Created User
     * @throws InvalidVerificationCodeException if code is invalid
     * @throws VerificationCodeExpiredException if code has expired
     */
    fun verifyEmailAndCreateUser(code: String): User {
        // Find pending user by verification code
        val pendingUser =
            pendingUserRepository.findByVerificationCode(code)
                ?: throw InvalidVerificationCodeException()

        // Check if code has expired
        if (Instant.now().isAfter(pendingUser.expiresAt)) {
            pendingUserRepository.delete(pendingUser)
            throw VerificationCodeExpiredException()
        }

        // Create actual user
        val user =
            User(
                email = pendingUser.email,
                name = pendingUser.name,
                passwordHash = pendingUser.passwordHash,
            )

        val savedUser = userRepository.save(user)

        // Delete pending user
        pendingUserRepository.delete(pendingUser)

        return savedUser
    }

    /**
     * Resends verification email for a pending user
     * @param email Email address
     * @throws InvalidVerificationCodeException if no pending user found
     */
    fun resendVerificationEmail(email: String) {
        val pendingUser =
            pendingUserRepository.findByEmail(email)
                ?: throw InvalidVerificationCodeException()

        // Generate new verification code
        val newCode = generateVerificationCode()
        val newExpiresAt = Instant.now().plusSeconds(VERIFICATION_CODE_EXPIRATION_HOURS * 3600)

        pendingUser.verificationCode = newCode
        pendingUser.expiresAt = newExpiresAt

        pendingUserRepository.save(pendingUser)

        // Send new verification email
        emailService.sendVerificationEmail(email, newCode)
    }

    /**
     * Cleans up expired pending users
     * This should be called periodically (e.g., via scheduled task)
     */
    fun cleanupExpiredPendingUsers() {
        pendingUserRepository.deleteByExpiresAtBefore(Instant.now())
    }

    /**
     * Generates a random verification code
     */
    private fun generateVerificationCode(): String = UUID.randomUUID().toString()
}
