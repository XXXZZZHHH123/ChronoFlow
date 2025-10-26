package nus.edu.u.system.enums.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionTypeEnumTest {

    @Test
    void array_containsAllPermissionCodes() {
        assertThat(PermissionTypeEnum.MENU.array()).containsExactly(1, 2, 3);
    }

    @Test
    void eachEnumExposesTypeAndName() {
        assertThat(PermissionTypeEnum.MENU.getType()).isEqualTo(1);
        assertThat(PermissionTypeEnum.API.getTypeName()).isEqualTo("API permission");
    }
}
