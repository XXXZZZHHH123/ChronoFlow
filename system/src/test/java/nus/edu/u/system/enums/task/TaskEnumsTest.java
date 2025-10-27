package nus.edu.u.system.enums.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskEnumsTest {

    @Test
    void taskStatusEnum_arrayIncludesEveryCode() {
        assertThat(TaskStatusEnum.PENDING.array()).containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    void taskStatusEnum_getEnumReturnsDisplayName() {
        assertThat(TaskStatusEnum.getEnum(2)).isEqualTo("Completed");
        assertThat(TaskStatusEnum.getEnum(99)).isNull();
    }

    @Test
    void taskStatusEnum_fromStatusOrDefault_handlesNullAndUnknown() {
        assertThat(TaskStatusEnum.fromStatusOrDefault(null)).isEqualTo(TaskStatusEnum.PENDING);
        assertThat(TaskStatusEnum.fromStatusOrDefault(3)).isEqualTo(TaskStatusEnum.DELAYED);
        assertThat(TaskStatusEnum.fromStatusOrDefault(42)).isNull();
    }

    @Test
    void taskActionEnum_arrayContainsAllCodes() {
        assertThat(TaskActionEnum.CREATE.array()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void taskActionEnum_getEnumMapsCodesToActions() {
        assertThat(TaskActionEnum.getEnum(4)).isEqualTo(TaskActionEnum.UPDATE);
        assertThat(TaskActionEnum.getEnum(-1)).isNull();
    }

    @Test
    void taskActionEnum_updateActionsAreWhitelisted() {
        assertThat(TaskActionEnum.getUpdateTaskAction())
                .containsExactlyInAnyOrder(2, 4, 5, 6, 7, 8, 9);
    }
}
