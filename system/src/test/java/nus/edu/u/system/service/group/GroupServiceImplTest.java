package nus.edu.u.system.service.group;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.enums.ErrorCodeConstants;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@EnableAspectJAutoProxy(exposeProxy = true)
@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private GroupService groupServiceProxy;

    @InjectMocks
    private GroupServiceImpl groupService;

    private CreateGroupReqVO createGroupReqVO;
    private UpdateGroupReqVO updateGroupReqVO;
    private DeptDO deptDO;
    private UserDO userDO;
    private EventDO eventDO;

    @BeforeEach
    void setUp() {
        createGroupReqVO = new CreateGroupReqVO();
        createGroupReqVO.setName("Test Group");
        createGroupReqVO.setEventId(1L);
        createGroupReqVO.setLeadUserId(1L);
        createGroupReqVO.setPhone("12345678901");
        createGroupReqVO.setEmail("test@example.com");
        createGroupReqVO.setRemark("Test remark");
        createGroupReqVO.setSort(1);

        updateGroupReqVO = new UpdateGroupReqVO();
        updateGroupReqVO.setId(1L);
        updateGroupReqVO.setName("Updated Group");

        deptDO = DeptDO.builder()
                .id(1L)
                .name("Test Group")
                .eventId(1L)
                .leadUserId(1L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();

        userDO = UserDO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();

        eventDO = EventDO.builder()
                .id(1L)
                .name("Test Event")
                .build();
    }

    @Test
    void createGroup_Success() {
        // Given
        when(eventMapper.selectById(1L)).thenReturn(eventDO);
        when(deptMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(userDO);
        when(deptMapper.insert(any(DeptDO.class))).thenAnswer(invocation -> {
            DeptDO dept = invocation.getArgument(0);
            dept.setId(1L);
            return 1;
        });

        // When
        Long result = groupService.createGroup(createGroupReqVO);

        // Then
        assertEquals(1L, result);
        verify(eventMapper).selectById(1L);
        verify(deptMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(userMapper).selectById(1L);
        verify(deptMapper).insert(any(DeptDO.class));
    }

    @Test
    void createGroup_EventNotFound() {
        // Given
        when(eventMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.createGroup(createGroupReqVO));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
        verify(eventMapper).selectById(1L);
        verify(deptMapper, never()).insert(any());
    }

    @Test
    void createGroup_GroupNameExists() {
        // Given
        when(eventMapper.selectById(1L)).thenReturn(eventDO);
        when(deptMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(deptDO);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.createGroup(createGroupReqVO));

        assertEquals(ErrorCodeConstants.GROUP_NAME_EXISTS.getCode(), exception.getCode());
        verify(deptMapper, never()).insert(any());
    }

    @Test
    void createGroup_LeadUserNotFound() {
        // Given
        when(eventMapper.selectById(1L)).thenReturn(eventDO);
        when(deptMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.createGroup(createGroupReqVO));

        assertEquals(ErrorCodeConstants.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(deptMapper, never()).insert(any());
    }

    @Test
    void updateGroup_Success() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(deptDO);
        when(deptMapper.updateById(any(DeptDO.class))).thenReturn(1);

        // When
        assertDoesNotThrow(() -> groupService.updateGroup(updateGroupReqVO));

        // Then
        verify(deptMapper).selectById(1L);
        verify(deptMapper).updateById(any(DeptDO.class));
    }

    @Test
    void updateGroup_GroupNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.updateGroup(updateGroupReqVO));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
        verify(deptMapper, never()).updateById(any());
    }

    @Test
    void deleteGroup_Success() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(deptDO);
        lenient().when(groupService.getGroupMembers(1L)).thenReturn(Collections.emptyList());
        when(deptMapper.update(isNull(), any())).thenReturn(1);

        // When
        assertDoesNotThrow(() -> groupService.deleteGroup(1L));

        // Then
        verify(deptMapper).selectById(1L);
        verify(deptMapper).update(isNull(), any());
    }


    @Test
    void deleteGroup_GroupNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.deleteGroup(1L));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
        verify(deptMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void addMemberToGroup_Success() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .username("testuser2")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .deptId(null) // Not in any group
                .build();

        when(deptMapper.selectById(groupId)).thenReturn(deptDO);
        when(userMapper.selectById(userId)).thenReturn(user);
        when(userMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        assertDoesNotThrow(() -> groupService.addMemberToGroup(groupId, userId));

        // Then
        verify(deptMapper).selectById(groupId);
        verify(userMapper).selectById(userId);
        verify(userMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void addMemberToGroup_GroupNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.addMemberToGroup(1L, 2L));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void addMemberToGroup_UserNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(deptDO);
        when(userMapper.selectById(2L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.addMemberToGroup(1L, 2L));

        assertEquals(ErrorCodeConstants.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void addMemberToGroup_UserAlreadyInGroup() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .deptId(groupId) // Already in the group
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();

        when(deptMapper.selectById(groupId)).thenReturn(deptDO);
        when(userMapper.selectById(userId)).thenReturn(user);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.addMemberToGroup(groupId, userId));

        assertEquals(ErrorCodeConstants.GROUP_MEMBER_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void addMemberToGroup_UserDisabled() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();

        when(deptMapper.selectById(groupId)).thenReturn(deptDO);
        when(userMapper.selectById(userId)).thenReturn(user);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.addMemberToGroup(groupId, userId));

        assertEquals(ErrorCodeConstants.USER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void removeMemberFromGroup_Success() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .deptId(groupId)
                .build();
        DeptDO group = DeptDO.builder()
                .id(groupId)
                .leadUserId(3L) // Different user is leader
                .build();

        when(userMapper.selectById(userId)).thenReturn(user);
        when(deptMapper.selectById(groupId)).thenReturn(group);
        when(userMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        assertDoesNotThrow(() -> groupService.removeMemberFromGroup(groupId, userId));

        // Then
        verify(userMapper).selectById(userId);
        verify(deptMapper).selectById(groupId);
        verify(userMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void removeMemberFromGroup_UserNotInGroup() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .deptId(2L) // In different group
                .build();

        when(userMapper.selectById(userId)).thenReturn(user);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.removeMemberFromGroup(groupId, userId));

        assertEquals(ErrorCodeConstants.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void removeMemberFromGroup_CannotRemoveLeader() {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        UserDO user = UserDO.builder()
                .id(userId)
                .deptId(groupId)
                .build();
        DeptDO group = DeptDO.builder()
                .id(groupId)
                .leadUserId(userId) // User is the leader
                .build();

        when(userMapper.selectById(userId)).thenReturn(user);
        when(deptMapper.selectById(groupId)).thenReturn(group);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.removeMemberFromGroup(groupId, userId));

        assertEquals(ErrorCodeConstants.CANNOT_REMOVE_GROUP_LEADER.getCode(), exception.getCode());
    }

    @Test
    void getGroupMembers_Success() {
        // Given
        Long groupId = 1L;
        List<UserDO> users = Arrays.asList(
                UserDO.builder()
                        .id(1L)
                        .username("user1")
                        .email("user1@example.com")
                        .phone("12345678901")
                        .deptId(groupId)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build(),
                UserDO.builder()
                        .id(2L)
                        .username("user2")
                        .email("user2@example.com")
                        .phone("12345678902")
                        .deptId(groupId)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build()
        );

        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(users);

        // When
        List<GroupRespVO.MemberInfo> result = groupService.getGroupMembers(groupId);

        // Then
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        verify(userMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void addMembersToGroup_Success() {
        // Given
        Long groupId = 1L;
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);

        when(deptMapper.selectById(groupId)).thenReturn(deptDO);

        lenient().when(userMapper.selectById(anyLong())).thenReturn(UserDO.builder()
                .id(1L)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());
        lenient().when(userMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // When
        assertDoesNotThrow(() -> groupService.addMembersToGroup(groupId, userIds));

        // Then
        verify(deptMapper, times(4)).selectById(groupId);
    }

    @Test
    void addMembersToGroup_GroupNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.addMembersToGroup(1L, Arrays.asList(1L, 2L)));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void addMembersToGroup_EmptyUserIds() {
        // Given & When
        assertDoesNotThrow(() -> groupService.addMembersToGroup(1L, Collections.emptyList()));

        // Then
        verify(deptMapper, never()).selectById(any());
    }

    @Test
    void removeMembersToGroup_Success() {
        // Given
        Long groupId = 1L;
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);

        when(deptMapper.selectById(groupId)).thenReturn(deptDO);

        // Mock AopContext to return the service itself
        try (MockedStatic<AopContext> mockedAopContext = mockStatic(AopContext.class)) {
            mockedAopContext.when(AopContext::currentProxy).thenReturn(groupService);

            // When
            assertDoesNotThrow(() -> groupService.removeMembersToGroup(groupId, userIds));

            // Then
            verify(deptMapper).selectById(groupId);
        }
    }

    @Test
    void removeMembersToGroup_GroupNotFound() {
        // Given
        when(deptMapper.selectById(1L)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> groupService.removeMembersToGroup(1L, Arrays.asList(1L, 2L)));

        assertEquals(ErrorCodeConstants.GROUP_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void removeMembersToGroup_EmptyUserIds() {
        // Given & When
        assertDoesNotThrow(() -> groupService.removeMembersToGroup(1L, Collections.emptyList()));

        // Then
        verify(deptMapper, never()).selectById(any());
    }
}