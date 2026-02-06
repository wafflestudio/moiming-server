package com.wafflestudio.spring2025.domain.auth.service

import com.wafflestudio.spring2025.common.email.service.EmailService
import com.wafflestudio.spring2025.domain.auth.exception.AccountAlreadyExistsException
import com.wafflestudio.spring2025.domain.auth.exception.AuthErrorCode
import com.wafflestudio.spring2025.domain.auth.exception.AuthValidationException
import com.wafflestudio.spring2025.domain.auth.model.SocialProvider
import com.wafflestudio.spring2025.domain.user.identity.repository.UserIdentityRepository
import com.wafflestudio.spring2025.domain.user.model.PendingUser
import com.wafflestudio.spring2025.domain.user.model.User
import com.wafflestudio.spring2025.domain.user.repository.PendingUserRepository
import com.wafflestudio.spring2025.domain.user.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.collections.forEach
import kotlin.collections.ifEmpty

@Service
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val pendingUserRepository: PendingUserRepository,
    private val emailService: EmailService,
    private val identityRepository: UserIdentityRepository,
) {
    companion object {
        private const val VERIFICATION_CODE_EXPIRATION_HOURS = 24L
    }

    /**
     * 대기 중인 사용자 등록을 생성하고 인증 이메일을 발송합니다
     * @param email 사용자 이메일 주소
     * @param name 사용자 이름
     * @param passwordHash 해시된 비밀번호
     * @throws EmailAccountAlreadyExistsException 이메일이 users 또는 pending_users에 이미 존재하는 경우
     */
    fun createPendingUser(
        email: String,
        name: String,
        password: String,
    ) {
        validateEmail(email)
        validateName(name)
        validatePassword(password)

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        pendingUserRepository.findByEmail(email)?.let { existingPendingUser ->
            if (Instant.now().isAfter(existingPendingUser.expiresAt)) {
                pendingUserRepository.delete(existingPendingUser)
            } else {
                throw AccountAlreadyExistsException(AuthErrorCode.EMAIL_VERIFICATION_PENDING)
            }
        }

        userRepository.findByEmail(email)?.let { user ->
            identityRepository
                .findByUserId(user.id!!)
                .ifEmpty {
                    throw AccountAlreadyExistsException(AuthErrorCode.EMAIL_ACCOUNT_ALREADY_EXIST)
                }.forEach { identity ->
                    when (identity.provider) {
                        SocialProvider.GOOGLE.name -> throw AccountAlreadyExistsException(AuthErrorCode.GOOGLE_ACCOUNT_ALREADY_EXIST)
                    }
                }
            throw AccountAlreadyExistsException(AuthErrorCode.EMAIL_ACCOUNT_ALREADY_EXIST)
        }

        // Generate verification code
        val verificationCode = generateVerificationCode()

        // Send verification email
        emailService.sendVerificationEmail(email, verificationCode)

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
    }

    /**
     * Verifies the verification code and creates a verified user
     * @param code Verification code from email
     * @return Created User
     * @throws InvalidVerificationCodeException if code is invalid or expired
     */
    fun verifyEmailAndCreateUser(code: String): User {
        // Find pending user by verification code
        val pendingUser =
            pendingUserRepository.findByVerificationCode(code)
                ?: throw AuthValidationException(AuthErrorCode.INVALID_VERIFICATION_CODE)

        // Check if code has expired
        if (Instant.now().isAfter(pendingUser.expiresAt)) {
            pendingUserRepository.delete(pendingUser)
            throw AuthValidationException(AuthErrorCode.INVALID_VERIFICATION_CODE)
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
                ?: throw AuthValidationException(AuthErrorCode.NO_SUCH_PENDING_USER)

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

    private fun validateEmail(email: String) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!email.matches(emailRegex)) {
            throw AuthValidationException(AuthErrorCode.BAD_EMAIL)
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
}
