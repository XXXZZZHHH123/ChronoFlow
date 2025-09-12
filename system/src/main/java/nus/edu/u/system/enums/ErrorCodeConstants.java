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
    ErrorCode AUTH_LOGIN_BAD_CREDENTIALS = new ErrorCode(10_01_001, "Login fail, username/password is wrong");
    ErrorCode AUTH_LOGIN_USER_DISABLED = new ErrorCode(10_01_002, "Login fail, this account is disabled");
    ErrorCode AUTH_LOGIN_CAPTCHA_CODE_ERROR = new ErrorCode(10_01_003, "Captcha wrong, reason：{}");
    ErrorCode REFRESH_TOKEN_WRONG = new ErrorCode(10_01_004, "Can't refresh, please login again");


    // ========= user crud module 12-01-000 ============
    ErrorCode USERNAME_EXIST = new ErrorCode(12_01_001, "Username already exists");
    ErrorCode EMAIL_EXIST = new ErrorCode(12_01_002, "Email already exists");
    ErrorCode WRONG_MOBILE = new ErrorCode(12_01_003, "Invalid mobile");
    ErrorCode INSERT_FAILURE = new ErrorCode(12_01_004, "Insert failure");
    ErrorCode PHONE_EXIST = new ErrorCode(12_01_005, "Phone already exists");
    ErrorCode USER_NOT_FOUND = new ErrorCode(12_01_006, "User not found");
    ErrorCode UPDATE_FAILURE = new ErrorCode(12_01_007, "Update failure");

    ErrorCode GROUP_NOT_FOUND = new ErrorCode(10_02_001, "Group not found");
    ErrorCode EVENT_NOT_FOUND = new ErrorCode(10_02_002, "Event not found");
    ErrorCode USER_NOT_FOUND = new ErrorCode(10_02_003, "User not found");
    ErrorCode GROUP_NAME_EXISTS = new ErrorCode(10_02_004, "Group name already exists in this event");
    ErrorCode GROUP_MEMBER_ALREADY_EXISTS = new ErrorCode(10_02_005, "User is already a member of this group");
    ErrorCode USER_STATUS_INVALID = new ErrorCode(10_02_006, "User status is invalid, cannot add to group");
    ErrorCode CANNOT_REMOVE_GROUP_LEADER = new ErrorCode(10_02_007, "Cannot remove group leader from group");



    // ========= Reg module 11-01-000 =============
    ErrorCode NO_SEARCH_RESULT = new ErrorCode(11_01_001, "No result found");
    ErrorCode REG_FAIL = new ErrorCode(11_01_002, "Sign up fail, please contact administrator");
    ErrorCode EXCEED_MAX_RETRY_GENERATE_CODE = new ErrorCode(11_01_003, "Failed to generate unique organization code, please try again");
    ErrorCode ACCOUNT_EXIST = new ErrorCode(11_01_004, "Account already exist");
}
