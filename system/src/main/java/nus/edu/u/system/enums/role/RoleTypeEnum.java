package nus.edu.u.system.enums.role;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * Role type enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum RoleTypeEnum implements ArrayValuable<Integer> {

  /** System role */
  SYSTEM(1, "System role"),
  /** Custom role */
  CUSTOM(2, "Custom role");

  private final Integer type;

  private final String typeName;

  private static final Integer[] ARRAYS =
      Arrays.stream(values()).map(RoleTypeEnum::getType).toArray(Integer[]::new);

  @Override
  public Integer[] array() {
    return ARRAYS;
  }
}
