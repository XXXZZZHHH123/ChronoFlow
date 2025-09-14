package nus.edu.u.system.enums.user;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * @author Lu Shuwen
 * @date 2025-09-11
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements ArrayValuable<Integer> {
  ENABLE(0, "Enable"),

  DISABLE(1, "Disable"),

  /** Pending for sign up */
  PENDING(2, "Pending");

  public static final Integer[] ARRAYS =
      Arrays.stream(values()).map(UserStatusEnum::getCode).toArray(Integer[]::new);

  private final Integer code;

  private final String action;

  @Override
  public Integer[] array() {
    return ARRAYS;
  }
}
