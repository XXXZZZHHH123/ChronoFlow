package nus.edu.u.system.enums.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;
import nus.edu.u.system.enums.task.TaskActionEnum;

import java.util.Arrays;

/**
 * @author Lu Shuwen
 * @date 2025-09-11
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements ArrayValuable<Integer> {

    ENABLE(0, "Enable"),

    DISABLE(1, "Disable"),

    /**
     * Pending for sign up
     */
    PENDING(2, "Pending");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(UserStatusEnum::getCode).toArray(Integer[]::new);

    private final Integer code;

    private final String action;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
