package nus.edu.u.system.enums.role;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTypeEnumTest {

    @Test
    void array_containsSystemAndCustomCodes() {
        assertThat(RoleTypeEnum.SYSTEM.array()).containsExactly(1, 2);
    }

    @Test
    void gettersExposeMetadata() {
        assertThat(RoleTypeEnum.CUSTOM.getType()).isEqualTo(2);
        assertThat(RoleTypeEnum.SYSTEM.getTypeName()).isEqualTo("System role");
    }
}
