package nus.edu.u.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import nus.edu.u.common.exception.enums.GlobalErrorCodeConstants;

/**
 * Error code object
 *
 * <p>Global error code，occupied [0, 999], see {@link GlobalErrorCodeConstants} Service error code
 * like 10-01-100
 */
@Data
@AllArgsConstructor
public class ErrorCode {

  /** Error code */
  private final Integer code;

  /** Error message */
  private final String msg;
}
