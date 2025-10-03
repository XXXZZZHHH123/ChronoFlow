package nus.edu.u.system.enums.task;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
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
    CREATE(1, "Create"),

    ASSIGN(2, "Assign"),

    DELETE(3, "Delete"),

    UPDATE(4, "Update"),

    SUBMIT(5, "Submit"),

    BLOCK(6, "Block"),

    ACCEPT(7, "Accept"),

    REJECT(8, "Reject"),

    APPROVE(9, "Approve");

    private static final Map<Integer, TaskActionEnum> CODE_MAP =
            Arrays.stream(TaskActionEnum.values())
                    .collect(Collectors.toMap(TaskActionEnum::getCode, t -> t));

    public static final Integer[] ARRAYS =
            Arrays.stream(values()).map(TaskActionEnum::getCode).toArray(Integer[]::new);

    private final Integer code;

    private final String action;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TaskActionEnum getEnum(Integer code) {
        if (CODE_MAP.containsKey(code)) {
            return CODE_MAP.get(code);
        }
        return null;
    }

    public static Integer[] getUpdateTaskAction() {
        return new Integer[] {
            APPROVE.getCode(),
            UPDATE.getCode(),
            SUBMIT.getCode(),
            BLOCK.getCode(),
            ACCEPT.getCode(),
            REJECT.getCode(),
            APPROVE.getCode(),
        };
    }
}
