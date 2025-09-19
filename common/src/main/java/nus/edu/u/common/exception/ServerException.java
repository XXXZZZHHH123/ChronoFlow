package nus.edu.u.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nus.edu.u.common.exception.enums.GlobalErrorCodeConstants;

/** Server exception */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ServerException extends RuntimeException {

    /**
     * Global error code
     *
     * @see GlobalErrorCodeConstants
     */
    private Integer code;

    /** Error message */
    private String message;

    /** 空构造方法，避免反序列化问题 */
    public ServerException() {}

    public ServerException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }

    public ServerException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public ServerException setCode(Integer code) {
        this.code = code;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ServerException setMessage(String message) {
        this.message = message;
        return this;
    }
}
