package nus.edu.u.system.enums.task;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum TaskActionEnum implements ArrayValuable<Integer> {
  ASSIGN(1, "Assign");

  public static final Integer[] ARRAYS =
      Arrays.stream(values()).map(TaskActionEnum::getCode).toArray(Integer[]::new);

  private final Integer code;

  private final String action;

  @Override
  public Integer[] array() {
    return ARRAYS;
  }
}
