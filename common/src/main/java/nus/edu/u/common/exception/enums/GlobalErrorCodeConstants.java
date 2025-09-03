package nus.edu.u.common.exception.enums;

import nus.edu.u.common.exception.ErrorCode;

/**
 * Global error code enum
 * 0-999 System error code occupied
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */

public interface GlobalErrorCodeConstants {

    ErrorCode SUCCESS = new ErrorCode(0, "Success");

    // ========== Client error segment ==========

    ErrorCode BAD_REQUEST = new ErrorCode(400, "Invalid request parameters");
    ErrorCode UNAUTHORIZED = new ErrorCode(401, "Account unauthorized");
    ErrorCode FORBIDDEN = new ErrorCode(403, "No permission for this operation");
    ErrorCode NOT_FOUND = new ErrorCode(404, "Not found");
    ErrorCode METHOD_NOT_ALLOWED = new ErrorCode(405, "Invalid request method");
    ErrorCode EXPIRED_LOGIN_CREDENTIALS = new ErrorCode(406, "Login expired, please log in again");
    ErrorCode MISSING_COOKIE = new ErrorCode(410, "Missing cookie");
    ErrorCode LOCKED = new ErrorCode(423, "Request failed, please try again later");
    ErrorCode TOO_MANY_REQUESTS = new ErrorCode(429, "The request is too frequent, please try again later");

    // ========== Server error segment ==========

    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "System error");
    ErrorCode NOT_IMPLEMENTED = new ErrorCode(501, "Function not implemented/not enabled");
    ErrorCode ERROR_CONFIGURATION = new ErrorCode(502, "Error configuration");

    // ========== Customise error segment ==========

    ErrorCode REPEATED_REQUESTS = new ErrorCode(900, "Repeat request, please try again later"); // 重复请求
    ErrorCode DEMO_DENY = new ErrorCode(901, "Demo mode, write operation disabled");

    ErrorCode UNKNOWN = new ErrorCode(999, "Unknown error");

}
