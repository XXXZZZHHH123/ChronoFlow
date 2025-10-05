package nus.edu.u.system.enums.task;

import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * task status enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum implements ArrayValuable<Integer> {
    PENDING(0, "Pending"),
    PROGRESS(1, "Progress"),
    COMPLETED(2, "Completed"),
    DELAYED(3, "Delayed"),
    BLOCKED(4, "Blocked"),
    ACCEPTED(5, "Accepted"),
    PENDING_APPROVAL(6, "Pending approval"),
    REJECTED(7, "Rejected");

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(TaskStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;

    private final String name;

    public static TaskStatusEnum fromStatusOrDefault(Integer status) {
        if (status == null) {
            return PENDING;
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
