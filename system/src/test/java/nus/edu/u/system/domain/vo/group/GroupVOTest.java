package nus.edu.u.system.domain.vo.group;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupVOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // =============== CreateGroupReqVO Tests ===============

    @Test
    void createGroupReqVO_ValidData_ShouldPass() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName("Test Group");
        vo.setEventId(1L);
        vo.setLeadUserId(1L);
        vo.setRemark("Test remark");
        vo.setSort(1);

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void createGroupReqVO_BlankName_ShouldFail() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName(""); // Invalid: blank name
        vo.setEventId(1L);

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Group name cannot be empty")));
    }

    @Test
    void createGroupReqVO_NullName_ShouldFail() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName(null); // Invalid: null name
        vo.setEventId(1L);

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Group name cannot be empty")));
    }

    @Test
    void createGroupReqVO_NameTooLong_ShouldFail() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName("a".repeat(51)); // Invalid: exceeds 50 characters
        vo.setEventId(1L);

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains(
                                                        "Group name cannot exceed 50 characters")));
    }

    @Test
    void createGroupReqVO_NullEventId_ShouldFail() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName("Test Group");
        vo.setEventId(null); // Invalid: null event ID

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Event ID cannot be null")));
    }

    @Test
    void createGroupReqVO_RemarkTooLong_ShouldFail() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();
        vo.setName("Test Group");
        vo.setEventId(1L);
        vo.setRemark("a".repeat(256)); // Invalid: exceeds 255 characters

        // When
        Set<ConstraintViolation<CreateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains("Remark cannot exceed 255 characters")));
    }

    @Test
    void createGroupReqVO_DefaultSort_ShouldBeZero() {
        // Given
        CreateGroupReqVO vo = new CreateGroupReqVO();

        // When & Then
        assertEquals(0, vo.getSort());
    }

    // =============== UpdateGroupReqVO Tests ===============

    @Test
    void updateGroupReqVO_ValidData_ShouldPass() {
        // Given
        UpdateGroupReqVO vo = new UpdateGroupReqVO();
        vo.setId(1L);
        vo.setName("Updated Group");
        vo.setLeadUserId(2L);
        vo.setRemark("Updated remark");
        vo.setSort(2);
        vo.setStatus(1);

        // When
        Set<ConstraintViolation<UpdateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateGroupReqVO_NullId_ShouldFail() {
        // Given
        UpdateGroupReqVO vo = new UpdateGroupReqVO();
        vo.setId(null); // Invalid: null ID
        vo.setName("Updated Group");

        // When
        Set<ConstraintViolation<UpdateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Group ID cannot be null")));
    }

    @Test
    void updateGroupReqVO_NameTooLong_ShouldFail() {
        // Given
        UpdateGroupReqVO vo = new UpdateGroupReqVO();
        vo.setId(1L);
        vo.setName("a".repeat(51)); // Invalid: exceeds 50 characters

        // When
        Set<ConstraintViolation<UpdateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains(
                                                        "Group name cannot exceed 50 characters")));
    }

    @Test
    void updateGroupReqVO_RemarkTooLong_ShouldFail() {
        // Given
        UpdateGroupReqVO vo = new UpdateGroupReqVO();
        vo.setId(1L);
        vo.setRemark("a".repeat(256)); // Invalid: exceeds 255 characters

        // When
        Set<ConstraintViolation<UpdateGroupReqVO>> violations = validator.validate(vo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .contains("Remark cannot exceed 255 characters")));
    }

    // =============== GroupListReqVO Tests ===============

    @Test
    void groupListReqVO_DefaultValues_ShouldBeCorrect() {
        // Given
        GroupListReqVO vo = new GroupListReqVO();

        // When & Then
        assertEquals(1, vo.getPageNo());
        assertEquals(10, vo.getPageSize());
    }

    @Test
    void groupListReqVO_AllFields_ShouldWork() {
        // Given
        GroupListReqVO vo = new GroupListReqVO();
        vo.setName("Test Group");
        vo.setEventId(1L);
        vo.setStatus(1);
        vo.setLeadUserId(2L);
        vo.setPageNo(2);
        vo.setPageSize(20);

        // When & Then
        assertEquals("Test Group", vo.getName());
        assertEquals(1L, vo.getEventId());
        assertEquals(1, vo.getStatus());
        assertEquals(2L, vo.getLeadUserId());
        assertEquals(2, vo.getPageNo());
        assertEquals(20, vo.getPageSize());
    }

    // =============== GroupRespVO Tests ===============

    @Test
    void groupRespVO_AllFields_ShouldWork() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        GroupRespVO.MemberInfo member1 =
                GroupRespVO.MemberInfo.builder()
                        .userId(1L)
                        .username("user1")
                        .email("user1@example.com")
                        .phone("12345678901")
                        .roleId(1L)
                        .roleName("Member")
                        .joinTime(now)
                        .build();

        GroupRespVO.MemberInfo member2 =
                GroupRespVO.MemberInfo.builder()
                        .userId(2L)
                        .username("user2")
                        .email("user2@example.com")
                        .phone("12345678902")
                        .roleId(2L)
                        .roleName("Leader")
                        .joinTime(now.plusDays(1))
                        .build();

        GroupRespVO vo =
                GroupRespVO.builder()
                        .id(1L)
                        .name("Test Group")
                        .sort(1)
                        .leadUserId(2L)
                        .leadUserName("Leader User")
                        .remark("Test remark")
                        .status(1)
                        .statusName("Active")
                        .eventId(1L)
                        .eventName("Test Event")
                        .memberCount(2)
                        .members(Arrays.asList(member1, member2))
                        .createTime(now)
                        .updateTime(now.plusHours(1))
                        .build();

        // When & Then
        assertEquals(1L, vo.getId());
        assertEquals("Test Group", vo.getName());
        assertEquals(1, vo.getSort());
        assertEquals(2L, vo.getLeadUserId());
        assertEquals("Leader User", vo.getLeadUserName());
        assertEquals("Test remark", vo.getRemark());
        assertEquals(1, vo.getStatus());
        assertEquals("Active", vo.getStatusName());
        assertEquals(1L, vo.getEventId());
        assertEquals("Test Event", vo.getEventName());
        assertEquals(2, vo.getMemberCount());
        assertEquals(2, vo.getMembers().size());
        assertEquals(now, vo.getCreateTime());
        assertEquals(now.plusHours(1), vo.getUpdateTime());
    }

    @Test
    void memberInfo_AllFields_ShouldWork() {
        // Given
        LocalDateTime joinTime = LocalDateTime.now();
        GroupRespVO.MemberInfo memberInfo =
                GroupRespVO.MemberInfo.builder()
                        .userId(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .phone("12345678901")
                        .roleId(1L)
                        .roleName("Member")
                        .joinTime(joinTime)
                        .build();

        // When & Then
        assertEquals(1L, memberInfo.getUserId());
        assertEquals("testuser", memberInfo.getUsername());
        assertEquals("test@example.com", memberInfo.getEmail());
        assertEquals("12345678901", memberInfo.getPhone());
        assertEquals(1L, memberInfo.getRoleId());
        assertEquals("Member", memberInfo.getRoleName());
        assertEquals(joinTime, memberInfo.getJoinTime());
    }

    @Test
    void memberInfo_EqualsAndHashCode_ShouldWork() {
        // Given
        LocalDateTime joinTime = LocalDateTime.now();
        GroupRespVO.MemberInfo memberInfo1 =
                GroupRespVO.MemberInfo.builder()
                        .userId(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .phone("12345678901")
                        .roleId(1L)
                        .roleName("Member")
                        .joinTime(joinTime)
                        .build();

        GroupRespVO.MemberInfo memberInfo2 =
                GroupRespVO.MemberInfo.builder()
                        .userId(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .phone("12345678901")
                        .roleId(1L)
                        .roleName("Member")
                        .joinTime(joinTime)
                        .build();

        // When & Then
        assertEquals(memberInfo1, memberInfo2);
        assertEquals(memberInfo1.hashCode(), memberInfo2.hashCode());
    }

    @Test
    void memberInfo_ToString_ShouldWork() {
        // Given
        GroupRespVO.MemberInfo memberInfo =
                GroupRespVO.MemberInfo.builder().userId(1L).username("testuser").build();

        // When
        String toString = memberInfo.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("userId=1"));
        assertTrue(toString.contains("username=testuser"));
    }
}
