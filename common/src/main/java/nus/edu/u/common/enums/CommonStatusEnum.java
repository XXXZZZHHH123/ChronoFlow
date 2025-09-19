package nus.edu.u.common.enums;

import cn.hutool.core.util.ObjUtil;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nus.edu.u.common.core.ArrayValuable;

/**
 * Common status enum class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum implements ArrayValuable<Integer> {
    ENABLE(0, "Enable"),
    DISABLE(1, "Disable");

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(CommonStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;

    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isEnable(Integer status) {
        return ObjUtil.equal(ENABLE.status, status);
    }

    public static boolean isDisable(Integer status) {
        return ObjUtil.equal(DISABLE.status, status);
    }
}
