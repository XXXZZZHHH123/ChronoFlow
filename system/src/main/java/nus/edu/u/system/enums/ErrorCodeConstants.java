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
    ErrorCode AUTH_LOGIN_BAD_CREDENTIALS =
            new ErrorCode(10_01_001, "Login failed. Incorrect username or password");
    ErrorCode AUTH_LOGIN_USER_DISABLED =
            new ErrorCode(10_01_002, "Login failed. This account has been disabled");
    ErrorCode AUTH_LOGIN_CAPTCHA_CODE_ERROR =
            new ErrorCode(10_01_003, "Incorrect verification code. Please try again");
    ErrorCode REFRESH_TOKEN_WRONG =
            new ErrorCode(10_01_004, "Your session has expired. Please log in again");
    ErrorCode ACCOUNT_ERROR = new ErrorCode(10_01_005, "Account error");

    // ========= user crud module 12-01-000 ============
    ErrorCode USERNAME_EXIST = new ErrorCode(12_01_001, "Username already exists");
    ErrorCode EMAIL_EXIST = new ErrorCode(12_01_002, "Email already exists");
    ErrorCode WRONG_MOBILE = new ErrorCode(12_01_003, "Invalid mobile");
    ErrorCode USER_INSERT_FAILURE = new ErrorCode(12_01_004, "Insert failure");
    ErrorCode PHONE_EXIST = new ErrorCode(12_01_005, "Phone already exists");
    ErrorCode USER_NOTFOUND = new ErrorCode(12_01_006, "User not found");
    ErrorCode USER_DISABLE_FAILURE = new ErrorCode(12_01_014, "User disabled failure");
    ErrorCode USER_ENABLE_FAILURE = new ErrorCode(12_01_015, "User enable failure");
    ErrorCode UPDATE_FAILURE = new ErrorCode(12_01_007, "Update failure");
    ErrorCode USER_NOT_DELETED = new ErrorCode(12_01_008, "User not deleted");
    ErrorCode USER_ALREADY_DELETED = new ErrorCode(12_01_009, "User already deleted");
    ErrorCode USER_ALREADY_DISABLED = new ErrorCode(12_01_0010, "User already disabled");
    ErrorCode USER_ALREADY_ENABLED = new ErrorCode(12_01_0011, "User already enabled");
    ErrorCode ROLE_NOT_FOUND = new ErrorCode(12_01_0012, "Role not found");
    ErrorCode USER_ROLE_BIND_FAILURE = new ErrorCode(12_01_0013, "User role bind failure");
    ErrorCode EMAIL_BLANK = new ErrorCode(12_01_0014, "Email can not be blank");
    ErrorCode INVALID_EMAIL = new ErrorCode(12_01_0015, "Invalid email");
    ErrorCode EMPTY_ROLEIDS = new ErrorCode(12_01_0016, "roleIds can not be blank");
    ErrorCode NULL_USERID = new ErrorCode(12_01_0017, "Email existed but user not found on update");

    // ========= group module 10-02-000 ============
    ErrorCode GROUP_NOT_FOUND = new ErrorCode(10_02_001, "Group not found");
    ErrorCode EVENT_NOT_FOUND = new ErrorCode(10_02_002, "Event not found");
    ErrorCode USER_NOT_FOUND = new ErrorCode(10_02_003, "User not found");
    ErrorCode GROUP_NAME_EXISTS =
            new ErrorCode(10_02_004, "Group name already exists in this event");
    ErrorCode GROUP_MEMBER_ALREADY_EXISTS =
            new ErrorCode(10_02_005, "User is already a member of this group");
    ErrorCode USER_STATUS_INVALID =
            new ErrorCode(10_02_006, "User status is invalid, cannot add to group");
    ErrorCode CANNOT_REMOVE_GROUP_LEADER =
            new ErrorCode(10_02_007, "Cannot remove group leader from group");

    // ========= Reg module 11-01-000 =============
    ErrorCode NO_SEARCH_RESULT = new ErrorCode(11_01_001, "No matching result found");
    ErrorCode REG_FAIL = new ErrorCode(11_01_002, "Sign-up failed. Please contact support");
    ErrorCode EXCEED_MAX_RETRY_GENERATE_CODE =
            new ErrorCode(
                    11_01_003, "Unable to generate an organization code. Please try again later");
    ErrorCode ACCOUNT_EXIST =
            new ErrorCode(
                    11_01_004,
                    "This account already exists. Please log in or use a different account");

    // ========= Event module 13-01-000 =============
    ErrorCode ORGANIZER_NOT_FOUND = new ErrorCode(13_01_001, "Organizer does not exist");
    ErrorCode PARTICIPANT_NOT_FOUND = new ErrorCode(13_01_002, "Some participants do not exist");
    ErrorCode TIME_RANGE_INVALID =
            new ErrorCode(13_01_003, "The start time must be earlier than the end time");
    ErrorCode DUPLICATE_PARTICIPANTS =
            new ErrorCode(13_01_004, "The participant list contains duplicate users");
    ErrorCode EVENT_DELETE_FAILED = new ErrorCode(13_01_005, "Event deletion failed");
    ErrorCode EVENT_NOT_DELETED = new ErrorCode(13_01_006, "Event not deleted");
    ErrorCode EVENT_RESTORE_FAILED = new ErrorCode(13_01_006, "Event restore failed");

    // ========= Task module 13-02-000 ============
    ErrorCode TASK_STATUS_INVALID = new ErrorCode(13_02_001, "Illegal task status");
    ErrorCode TASK_ASSIGNEE_NOT_FOUND = new ErrorCode(13_02_002, "Assigned user does not exist");
    ErrorCode TASK_TIME_RANGE_INVALID =
            new ErrorCode(13_02_003, "The task start time must be earlier than the end time");
    ErrorCode TASK_ASSIGNEE_TENANT_MISMATCH =
            new ErrorCode(13_02_004, "The assigned user does not belong to this event");
    ErrorCode TASK_NOT_FOUND = new ErrorCode(13_02_005, "Task does not exist");
    ErrorCode TASK_TIME_OUTSIDE_EVENT =
            new ErrorCode(13_02_006, "The task timeframe must fall within the event timeframe");

    // ========= RolePermission module 14-01-000 ============
    ErrorCode CREATE_ROLE_FAILED = new ErrorCode(14_01_001, "Create role failed");
    ErrorCode CANNOT_FIND_ROLE = new ErrorCode(14_01_002, "Role not found");
    ErrorCode UPDATE_ROLE_FAILED = new ErrorCode(14_01_003, "Update role failed");
}
