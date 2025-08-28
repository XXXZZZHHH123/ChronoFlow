package nus.edu.u.common.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

import java.util.Arrays;

/**
 * Event status enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum EventStatusEnum implements ArrayValuable<Integer> {

    WAITING(1, "Waiting"),
    DOING(2, "Doing"),
    SUSPENDED(3, "Suspended"),
    DELAYED(4, "Delayed"),
    CLOSED(5, "Closed");

    private final Integer status;

    private final String name;

    private static final Integer[] ARRAYS = Arrays.stream(values()).map(EventStatusEnum::getStatus).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
