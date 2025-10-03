package nus.edu.u.system.domain.dataobject.dept;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import nus.edu.u.common.enums.CommonStatusEnum;
import org.junit.jupiter.api.Test;

class DeptDOTest {

    @Test
    void deptDO_AllFields_ShouldWork() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        DeptDO deptDO =
                DeptDO.builder()
                        .id(1L)
                        .name("Test Department")
                        .sort(1)
                        .leadUserId(2L)
                        .remark("Test remark")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .eventId(1L)
                        .build();

        // Set inherited fields
        deptDO.setTenantId(1L);
        deptDO.setCreateTime(now);
        deptDO.setUpdateTime(now);
        deptDO.setCreator("testuser");
        deptDO.setUpdater("testuser");
        deptDO.setDeleted(false);

        // When & Then
        assertEquals(1L, deptDO.getId());
        assertEquals("Test Department", deptDO.getName());
        assertEquals(1, deptDO.getSort());
        assertEquals(2L, deptDO.getLeadUserId());
        assertEquals("Test remark", deptDO.getRemark());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), deptDO.getStatus());
        assertEquals(1L, deptDO.getEventId());
        assertEquals(1L, deptDO.getTenantId());
        assertEquals(now, deptDO.getCreateTime());
        assertEquals(now, deptDO.getUpdateTime());
        assertEquals("testuser", deptDO.getCreator());
        assertEquals("testuser", deptDO.getUpdater());
        assertEquals(false, deptDO.getDeleted());
    }

    @Test
    void deptDO_Builder_ShouldWork() {
        // Given & When
        DeptDO deptDO =
                DeptDO.builder()
                        .id(1L)
                        .name("Test Department")
                        .sort(1)
                        .leadUserId(2L)
                        .remark("Test remark")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .eventId(1L)
                        .build();

        // Then
        assertNotNull(deptDO);
        assertEquals(1L, deptDO.getId());
        assertEquals("Test Department", deptDO.getName());
        assertEquals(1, deptDO.getSort());
        assertEquals(2L, deptDO.getLeadUserId());
        assertEquals("Test remark", deptDO.getRemark());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), deptDO.getStatus());
        assertEquals(1L, deptDO.getEventId());
    }

    @Test
    void deptDO_NoArgsConstructor_ShouldWork() {
        // Given & When
        DeptDO deptDO = new DeptDO();

        // Then
        assertNotNull(deptDO);
        assertNull(deptDO.getId());
        assertNull(deptDO.getName());
        assertNull(deptDO.getSort());
        assertNull(deptDO.getLeadUserId());
        assertNull(deptDO.getRemark());
        assertNull(deptDO.getStatus());
        assertNull(deptDO.getEventId());
    }

    @Test
    void deptDO_AllArgsConstructor_ShouldWork() {
        // Given & When
        DeptDO deptDO =
                new DeptDO(
                        1L, // id
                        "Test Department", // name
                        1, // sort
                        2L, // leadUserId
                        "Test remark", // remark
                        CommonStatusEnum.ENABLE.getStatus(), // status
                        1L // eventId
                        );

        // Then
        assertNotNull(deptDO);
        assertEquals(1L, deptDO.getId());
        assertEquals("Test Department", deptDO.getName());
        assertEquals(1, deptDO.getSort());
        assertEquals(2L, deptDO.getLeadUserId());
        assertEquals("Test remark", deptDO.getRemark());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), deptDO.getStatus());
        assertEquals(1L, deptDO.getEventId());
    }

    @Test
    void deptDO_SettersAndGetters_ShouldWork() {
        // Given
        DeptDO deptDO = new DeptDO();

        // When
        deptDO.setId(1L);
        deptDO.setName("Test Department");
        deptDO.setSort(1);
        deptDO.setLeadUserId(2L);
        deptDO.setRemark("Test remark");
        deptDO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        deptDO.setEventId(1L);

        // Then
        assertEquals(1L, deptDO.getId());
        assertEquals("Test Department", deptDO.getName());
        assertEquals(1, deptDO.getSort());
        assertEquals(2L, deptDO.getLeadUserId());
        assertEquals("Test remark", deptDO.getRemark());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), deptDO.getStatus());
        assertEquals(1L, deptDO.getEventId());
    }

    @Test
    void deptDO_EqualsAndHashCode_ShouldWork() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        DeptDO deptDO1 =
                DeptDO.builder()
                        .id(1L)
                        .name("Test Department")
                        .sort(1)
                        .leadUserId(2L)
                        .remark("Test remark")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .eventId(1L)
                        .build();
        deptDO1.setTenantId(1L);
        deptDO1.setCreateTime(now);
        deptDO1.setUpdateTime(now);
        deptDO1.setCreator("testuser");
        deptDO1.setUpdater("testuser");
        deptDO1.setDeleted(false);

        DeptDO deptDO2 =
                DeptDO.builder()
                        .id(1L)
                        .name("Test Department")
                        .sort(1)
                        .leadUserId(2L)
                        .remark("Test remark")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .eventId(1L)
                        .build();
        deptDO2.setTenantId(1L);
        deptDO2.setCreateTime(now);
        deptDO2.setUpdateTime(now);
        deptDO2.setCreator("testuser");
        deptDO2.setUpdater("testuser");
        deptDO2.setDeleted(false);

        // When & Then
        assertEquals(deptDO1, deptDO2);
        assertEquals(deptDO1.hashCode(), deptDO2.hashCode());
    }

    @Test
    void deptDO_EqualsAndHashCode_DifferentValues_ShouldNotBeEqual() {
        // Given
        DeptDO deptDO1 = DeptDO.builder().id(1L).name("Test Department 1").build();

        DeptDO deptDO2 = DeptDO.builder().id(2L).name("Test Department 2").build();

        // When & Then
        assertNotEquals(deptDO1, deptDO2);
        assertNotEquals(deptDO1.hashCode(), deptDO2.hashCode());
    }

    @Test
    void deptDO_ToString_ShouldWork() {
        // Given
        DeptDO deptDO =
                DeptDO.builder()
                        .id(1L)
                        .name("Test Department")
                        .sort(1)
                        .leadUserId(2L)
                        .remark("Test remark")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .eventId(1L)
                        .build();

        // When
        String toString = deptDO.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("name=Test Department"));
        assertTrue(toString.contains("sort=1"));
        assertTrue(toString.contains("leadUserId=2"));
        assertTrue(toString.contains("remark=Test remark"));
        assertTrue(toString.contains("status=0")); // ENABLE status value is 0
        assertTrue(toString.contains("eventId=1"));
    }

    @Test
    void deptDO_StatusValues_ShouldWork() {
        // Given
        DeptDO enabledDept = DeptDO.builder().status(CommonStatusEnum.ENABLE.getStatus()).build();

        DeptDO disabledDept = DeptDO.builder().status(CommonStatusEnum.DISABLE.getStatus()).build();

        // When & Then
        assertEquals(0, enabledDept.getStatus());
        assertEquals(1, disabledDept.getStatus());
    }
}
