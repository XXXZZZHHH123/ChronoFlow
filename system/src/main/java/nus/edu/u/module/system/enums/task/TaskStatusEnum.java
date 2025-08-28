package nus.edu.u.module.system.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

import java.util.Arrays;

/**
 * Task status enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum implements ArrayValuable<Integer> {

    WAITING(1, "Waiting"),
    DOING(2, "Doing"),
    DONE(3, "Done");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TaskStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;

    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
