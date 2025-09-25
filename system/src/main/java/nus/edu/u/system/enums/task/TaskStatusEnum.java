package nus.edu.u.system.enums.task;

import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * Task status enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum implements ArrayValuable<Integer> {
    WAITING(0, "Waiting"),
    DOING(1, "Doing"),
    DONE(2, "Done");

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(TaskStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;

    private final String name;

    public static TaskStatusEnum fromStatusOrDefault(Integer status) {
        if (status == null) {
            return WAITING;
        }
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.status, status))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
