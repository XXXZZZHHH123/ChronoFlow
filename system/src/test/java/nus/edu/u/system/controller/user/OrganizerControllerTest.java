package nus.edu.u.system.controller.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import nus.edu.u.system.convert.user.UserConvert;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.domain.dto.UpdateUserDTO;
import nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO;
import nus.edu.u.system.domain.vo.user.CreateUserReqVO;
import nus.edu.u.system.domain.vo.user.UpdateUserReqVO;
import nus.edu.u.system.domain.vo.user.UpdateUserRespVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.service.excel.ExcelService;
import nus.edu.u.system.service.user.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrganizerControllerTest {

    @Mock private UserService userService;
    @Mock private UserConvert userConvert;
    @Mock private ExcelService excelService;

    @InjectMocks private OrganizerController organizerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(organizerController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createUserForOrganizer_convertsRequestToDTO() throws Exception {
        CreateUserReqVO reqVO = new CreateUserReqVO();
        reqVO.setEmail("alice@example.com");
        reqVO.setRoleIds(List.of(1L));

        CreateUserDTO dto = CreateUserDTO.builder().email("alice@example.com").build();
        when(userConvert.toDTO(any(CreateUserReqVO.class))).thenReturn(dto);
        when(userService.createUserWithRoleIds(dto)).thenReturn(15L);

        mockMvc.perform(
                        post("/organizer/create/user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(15));

        verify(userService).createUserWithRoleIds(dto);
    }

    @Test
    void updateUserForOrganizer_mergesRoleInformation() throws Exception {
        UpdateUserReqVO reqVO = new UpdateUserReqVO();
        reqVO.setEmail("bob@example.com");
        reqVO.setRoleIds(List.of(2L));

        UserDO updated =
                UserDO.builder()
                        .id(7L)
                        .email("bob@example.com")
                        .status(1)
                        .remark("updated")
                        .build();
        UpdateUserRespVO respVO =
                UpdateUserRespVO.builder().id(7L).email("bob@example.com").build();

        when(userConvert.toDTO(any(UpdateUserReqVO.class))).thenReturn(new UpdateUserDTO());
        when(userService.updateUserWithRoleIds(any(UpdateUserDTO.class))).thenReturn(updated);
        when(userService.getAliveRoleIdsByUserId(7L)).thenReturn(List.of(2L, 3L));
        when(userConvert.toUpdateUserRespVO(updated)).thenReturn(respVO);

        mockMvc.perform(
                        patch("/organizer/update/user/{id}", 7L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.roleIds[0]").value(2));

        ArgumentCaptor<UpdateUserDTO> captor = ArgumentCaptor.forClass(UpdateUserDTO.class);
        verify(userService).updateUserWithRoleIds(captor.capture());
        Assertions.assertThat(captor.getValue().getId()).isEqualTo(7L);
    }

    @Test
    void bulkUpsertUsers_readsRowsFromExcel() throws Exception {
        List<CreateUserDTO> rows = List.of(CreateUserDTO.builder().email("a@b.com").build());
        BulkUpsertUsersRespVO respVO =
                BulkUpsertUsersRespVO.builder().totalRows(1).createdCount(1).build();
        when(excelService.parseCreateOrUpdateRows(any())).thenReturn(rows);
        when(userService.bulkUpsertUsers(rows)).thenReturn(respVO);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "users.xlsx", "application/vnd.ms-excel", new byte[] {1});

        mockMvc.perform(multipart("/organizer/users/bulk-upsert").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.createdCount").value(1));

        verify(userService).bulkUpsertUsers(rows);
    }

    @Test
    void softDeleteUser_delegatesToService() throws Exception {
        mockMvc.perform(delete("/organizer/delete/user/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        verify(userService).softDeleteUser(3L);
    }

    @Test
    void getAllUserProfiles_returnsList() throws Exception {
        UserProfileRespVO profile = new UserProfileRespVO();
        profile.setId(1L);
        when(userService.getAllUserProfiles()).thenReturn(List.of(profile));

        mockMvc.perform(get("/organizer/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }
}
