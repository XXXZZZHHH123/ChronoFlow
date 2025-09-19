package nus.edu.u.system.enums.permission;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Getter
@AllArgsConstructor
public enum PermissionTypeEnum implements ArrayValuable<Integer> {

    /** Menu permission */
    MENU(1, "Menu permission"),

    /** Button permission */
    BUTTON(2, "Button permission"),

    /** API permission */
    API(3, "API permission");

    private final Integer type;

    private final String typeName;

    private static final Integer[] ARRAYS =
            Arrays.stream(values()).map(PermissionTypeEnum::getType).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
