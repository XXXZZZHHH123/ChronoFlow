package nus.edu.u.system.enums.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserStatusEnumTest {

    @Test
    void array_returnsAllStatusCodes() {
        assertThat(UserStatusEnum.ENABLE.array()).containsExactly(0, 1, 2);
    }

    @Test
    void gettersExposeCodeAndAction() {
        assertThat(UserStatusEnum.DISABLE.getCode()).isEqualTo(1);
        assertThat(UserStatusEnum.PENDING.getAction()).isEqualTo("Pending");
    }
}
