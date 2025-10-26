package nus.edu.u.system.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.edu.u.system.domain.vo.reg.RegMemberReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchRespVO;
import nus.edu.u.system.enums.ErrorCodeConstants;
import nus.edu.u.system.service.user.RegService;
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
class RegControllerTest {

    @Mock private RegService regService;

    @InjectMocks private RegController regController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(regController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void search_returnsResult() throws Exception {
        RegSearchRespVO respVO =
                RegSearchRespVO.builder()
                        .organizationName("ChronoFlow")
                        .email("admin@example.com")
                        .build();
        when(regService.search(any(RegSearchReqVO.class))).thenReturn(respVO);

        RegSearchReqVO reqVO = RegSearchReqVO.builder().organizationId(1L).userId(2L).build();

        mockMvc.perform(
                        post("/system/reg/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("ChronoFlow"));

        verify(regService).search(any(RegSearchReqVO.class));
    }

    @Test
    void registerAsMember_successfulFlow() throws Exception {
        when(regService.registerAsMember(any(RegMemberReqVO.class))).thenReturn(true);

        RegMemberReqVO reqVO =
                RegMemberReqVO.builder()
                        .username("member01")
                        .password("password123")
                        .phone("91234567")
                        .build();

        mockMvc.perform(
                        post("/system/reg/member")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void registerAsMember_whenServiceFails_returnsError() throws Exception {
        when(regService.registerAsMember(any(RegMemberReqVO.class))).thenReturn(false);

        RegMemberReqVO reqVO =
                RegMemberReqVO.builder()
                        .username("member01")
                        .password("password123")
                        .phone("91234567")
                        .build();

        mockMvc.perform(
                        post("/system/reg/member")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodeConstants.REG_FAIL.getCode()))
                .andExpect(jsonPath("$.msg").value(ErrorCodeConstants.REG_FAIL.getMsg()));
    }

    @Test
    void registerAsOrganizer_successfulFlow() throws Exception {
        when(regService.registerAsOrganizer(any(RegOrganizerReqVO.class))).thenReturn(true);

        RegOrganizerReqVO reqVO =
                RegOrganizerReqVO.builder()
                        .name("Org")
                        .username("organizer")
                        .userPassword("password123")
                        .userEmail("org@example.com")
                        .mobile("98765432")
                        .organizationName("Org Inc")
                        .build();

        mockMvc.perform(
                        post("/system/reg/organizer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
