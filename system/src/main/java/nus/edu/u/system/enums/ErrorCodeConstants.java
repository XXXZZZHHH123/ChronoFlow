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

    ErrorCode GROUP_NOT_FOUND = new ErrorCode(10_02_001, "Group not found");
    ErrorCode EVENT_NOT_FOUND = new ErrorCode(10_02_002, "Event not found");
    ErrorCode USER_NOT_FOUND = new ErrorCode(10_02_003, "User not found");
    ErrorCode GROUP_NAME_EXISTS = new ErrorCode(10_02_004, "Group name already exists in this event");
    ErrorCode INSUFFICIENT_PERMISSION = new ErrorCode(10_02_005, "Insufficient permission to manage this group");



}
