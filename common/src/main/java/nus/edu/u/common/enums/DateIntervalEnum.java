package nus.edu.u.common.enums;

import cn.hutool.core.util.ArrayUtil;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * Enum of time intervals
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum DateIntervalEnum implements ArrayValuable<Integer> {
    DAY(1, "Days"),
    WEEK(2, "Weeks"),
    MONTH(3, "Months"),
    QUARTER(4, "Seasons"),
    YEAR(5, "Years");

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(DateIntervalEnum::getInterval).toArray(Integer[]::new);

    private final Integer interval;

    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static DateIntervalEnum valueOf(Integer interval) {
        return ArrayUtil.firstMatch(
                item -> item.getInterval().equals(interval), DateIntervalEnum.values());
    }
}
