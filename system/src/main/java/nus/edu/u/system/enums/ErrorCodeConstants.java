package nus.edu.u.system.enums;

import nus.edu.u.common.exception.ErrorCode;

/**
 * Error code enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
public interface ErrorCodeConstants {

    // ========= Auth module 10-01-000 ============
    ErrorCode AUTH_LOGIN_BAD_CREDENTIALS = new ErrorCode(10_01_001, "Login fail，username/password is wrong");
    ErrorCode AUTH_LOGIN_USER_DISABLED = new ErrorCode(10_01_002, "Login fail，this account is disabled");
    ErrorCode AUTH_LOGIN_CAPTCHA_CODE_ERROR = new ErrorCode(10_01_003, "Captcha wrong，reason：{}");
    ErrorCode REFRESH_TOKEN_WRONG = new ErrorCode(10_01_004, "Can't refresh, please login again");

    // ========= Reg module 11-01-000 =============
    ErrorCode NO_SEARCH_RESULT = new ErrorCode(11_01_001, "No result found");
    ErrorCode REG_FAIL = new ErrorCode(11_01_002, "Sign up fail, please contact administrator");
    ErrorCode EXCEED_MAX_RETRY_GENERATE_CODE = new ErrorCode(11_01_003, "Failed to generate unique organization code, please try again");
}
