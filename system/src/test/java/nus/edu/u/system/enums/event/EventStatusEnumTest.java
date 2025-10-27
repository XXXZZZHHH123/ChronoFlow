package nus.edu.u.system.enums.event;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EventStatusEnumTest {

    @Test
    void array_returnsStaticCodes() {
        assertThat(EventStatusEnum.NOT_STARTED.array()).containsExactly(0, 1, 2);
        assertThat(EventStatusEnum.ARRAYS).containsExactly(0, 1, 2);
    }

    @Test
    void fromCode_returnsMatchingEnum() {
        assertThat(EventStatusEnum.fromCode(1)).isEqualTo(EventStatusEnum.ACTIVE);
        assertThat(EventStatusEnum.fromCode(null)).isNull();
    }

    @Test
    void fromCode_invalidCodeThrows() {
        assertThatThrownBy(() -> EventStatusEnum.fromCode(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknow event status code");
    }
}
