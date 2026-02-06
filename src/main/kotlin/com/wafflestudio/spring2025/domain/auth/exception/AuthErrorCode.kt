package com.wafflestudio.spring2025.domain.auth.exception

import com.wafflestudio.spring2025.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatusCode: HttpStatus,
    override val title: String,
    override val message: String,
) : DomainErrorCode {
    // Auth
    BAD_EMAIL(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이메일 형식이 올바르지 않습니다.",
        message = "유효하지 않은 이메일 형식입니다.\n올바르게 입력하였는지 확인해 주세요.",
    ),
    BAD_PASSWORD(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "비밀번호 형식이 올바르지 않습니다.",
        message = "영문, 숫자를 포함하여\n8자리 이상으로 지정해주세요.",
    ),
    BAD_NAME(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "이름 형식이 올바르지 않습니다.",
        message = "최소 두 자리 이상 입력해주세요.",
    ),
    LOGIN_FAILED(
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        title = "로그인에 실패하였습니다.",
        message = "이메일 또는 비밀번호가 일치하지 않습니다.\n다시 확인해 주세요.",
    ),
    INVALID_VERIFICATION_CODE(
        httpStatusCode = HttpStatus.BAD_REQUEST,
        title = "유효하지 않은 인증 코드입니다.",
        message = "유효기간이 만료되었거나 잘못된 인증코드입니다.\n이메일 회원가입을 다시 시도해 주세요.",
    ),
    EMAIL_VERIFICATION_PENDING(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이메일 인증 대기 중입니다.",
        message = "이미 인증 메일이 발송되었습니다.\n메일함을 확인하여 인증을 완료해 주세요.",
    ),
    EMAIL_ACCOUNT_ALREADY_EXIST(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "이미 회원가입된 이메일입니다.",
        message = "다른 이메일을 사용하시거나,\n해당 이메일로 로그인을 시도해 주세요.",
    ),
    GOOGLE_ACCOUNT_ALREADY_EXIST(
        httpStatusCode = HttpStatus.CONFLICT,
        title = "소셜 회원가입된 이메일입니다.",
        message = "다른 이메일을 사용하시거나,\n해당 이메일로 소셜 로그인을 시도해 주세요.",
    ),
    AUTHENTICATION_REQUIRED(
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        title = "로그인이 필요합니다.",
        message = "비로그인 상태에서 이용할 수 없는 기능입니다.\n먼저 로그인을 해 주세요.",
    ),
    GOOGLE_OAUTH_ERROR(
        httpStatusCode = HttpStatus.UNAUTHORIZED,
        title = "구글 로그인에 실패하였습니다.",
        message = "잠시 후 다시 시도해주세요.\n오류가 지속되면 개발자에게 문의해 주세요.",
    ),
    NO_SUCH_PENDING_USER(
        httpStatusCode = HttpStatus.NOT_FOUND,
        title = "인증 요청 내역이 없습니다.",
        message = "기존에 이메일 인증을 요청한\n이력이 없습니다.",
    ),
}
