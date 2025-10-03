package nus.edu.u.system.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import nus.edu.u.system.domain.vo.group.AddMembersReqVO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.service.group.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock private GroupService groupService;

    @InjectMocks private GroupController groupController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groupController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createGroup_Success() throws Exception {
        // Given
        CreateGroupReqVO reqVO = new CreateGroupReqVO();
        reqVO.setName("Test Group");
        reqVO.setEventId(1L);
        reqVO.setLeadUserId(1L);
        reqVO.setRemark("Test remark");
        reqVO.setSort(1);

        Long expectedGroupId = 1L;
        when(groupService.createGroup(any(CreateGroupReqVO.class))).thenReturn(expectedGroupId);

        // When & Then
        mockMvc.perform(
                        post("/system/group/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(expectedGroupId));

        verify(groupService).createGroup(any(CreateGroupReqVO.class));
    }

    @Test
    void createGroup_InvalidInput() throws Exception {
        // Given - empty name
        CreateGroupReqVO reqVO = new CreateGroupReqVO();
        reqVO.setName(""); // Invalid: empty name
        reqVO.setEventId(1L);

        // When & Then
        mockMvc.perform(
                        post("/system/group/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isBadRequest());

        verify(groupService, never()).createGroup(any());
    }

    @Test
    void updateGroup_Success() throws Exception {
        // Given
        UpdateGroupReqVO reqVO = new UpdateGroupReqVO();
        reqVO.setId(1L);
        reqVO.setName("Updated Group");
        reqVO.setLeadUserId(2L);
        reqVO.setRemark("Updated remark");
        reqVO.setSort(2);
        reqVO.setStatus(1);

        doNothing().when(groupService).updateGroup(any(UpdateGroupReqVO.class));

        // When & Then
        mockMvc.perform(
                        put("/system/group/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).updateGroup(any(UpdateGroupReqVO.class));
    }

    @Test
    void deleteGroup_Success() throws Exception {
        // Given
        Long groupId = 1L;
        doNothing().when(groupService).deleteGroup(groupId);

        // When & Then
        mockMvc.perform(delete("/system/group/delete/{id}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).deleteGroup(groupId);
    }

    @Test
    void addMember_Success() throws Exception {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        doNothing().when(groupService).addMemberToGroup(groupId, userId);

        // When & Then
        mockMvc.perform(post("/system/group/{groupId}/members/{userId}", groupId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).addMemberToGroup(groupId, userId);
    }

    @Test
    void removeMember_Success() throws Exception {
        // Given
        Long groupId = 1L;
        Long userId = 2L;
        doNothing().when(groupService).removeMemberFromGroup(groupId, userId);

        // When & Then
        mockMvc.perform(delete("/system/group/{groupId}/members/{userId}", groupId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).removeMemberFromGroup(groupId, userId);
    }

    @Test
    void getGroupMembers_Success() throws Exception {
        // Given
        Long groupId = 1L;
        List<GroupRespVO.MemberInfo> members =
                Arrays.asList(
                        GroupRespVO.MemberInfo.builder()
                                .userId(1L)
                                .username("user1")
                                .email("user1@example.com")
                                .phone("12345678901")
                                .roleId(1L)
                                .roleName("Member")
                                .build(),
                        GroupRespVO.MemberInfo.builder()
                                .userId(2L)
                                .username("user2")
                                .email("user2@example.com")
                                .phone("12345678902")
                                .roleId(2L)
                                .roleName("Leader")
                                .build());

        when(groupService.getGroupMembers(groupId)).thenReturn(members);

        // When & Then
        mockMvc.perform(get("/system/group/{groupId}/members", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].username").value("user1"))
                .andExpect(jsonPath("$.data[1].userId").value(2))
                .andExpect(jsonPath("$.data[1].username").value("user2"));

        verify(groupService).getGroupMembers(groupId);
    }

    @Test
    void addMembers_Success() throws Exception {
        // Given
        Long groupId = 1L;
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);

        AddMembersReqVO reqVO = new AddMembersReqVO();
        reqVO.setUserIds(userIds);

        doNothing().when(groupService).addMembersToGroup(groupId, userIds);

        // When & Then
        mockMvc.perform(
                        post("/system/group/{groupId}/members/batch", groupId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).addMembersToGroup(groupId, userIds);
    }

    @Test
    void deleteMembers_Success() throws Exception {
        // Given
        Long groupId = 1L;
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        doNothing().when(groupService).removeMembersToGroup(groupId, userIds);

        // When & Then
        mockMvc.perform(
                        delete("/system/group/{groupId}/members/batch", groupId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        verify(groupService).removeMembersToGroup(groupId, userIds);
    }
}
