package nus.edu.u.system.controller.file;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.service.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileStorageService fileStorageService;

    @InjectMocks private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }

    @Test
    void uploadToTaskLog_returnsUploadResults() throws Exception {
        FileResultVO fileResultVO =
                FileResultVO.builder().name("report.pdf").objectName("task/1/report.pdf").build();
        when(fileStorageService.uploadToTaskLog(any(FileUploadReqVO.class)))
                .thenReturn(List.of(fileResultVO));

        MockMultipartFile file =
                new MockMultipartFile("files", "report.pdf", "application/pdf", new byte[] {1});

        mockMvc.perform(
                        multipart("/api/files/upload")
                                .file(file)
                                .param("taskLogId", "5")
                                .param("eventId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("report.pdf"));

        verify(fileStorageService).uploadToTaskLog(any(FileUploadReqVO.class));
    }

    @Test
    void downloadFileByTaskLogId_returnsSingleResult() throws Exception {
        FileResultVO fileResultVO = FileResultVO.builder().name("log.txt").build();
        when(fileStorageService.downloadFile(7L)).thenReturn(fileResultVO);

        mockMvc.perform(get("/api/files/{taskLogId}/download", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("log.txt"));
    }

    @Test
    void downloadFilesByTaskLogId_returnsList() throws Exception {
        FileResultVO fileResultVO = FileResultVO.builder().name("doc.txt").build();
        when(fileStorageService.downloadFilesByTaskLogId(9L)).thenReturn(List.of(fileResultVO));

        mockMvc.perform(get("/api/files/{taskLogId}/download-all", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("doc.txt"));
    }
}
