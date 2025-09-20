package nus.edu.u.system.service.excel;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.EXCEL_FORMAT_ERROR;
import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.EXCEL_HEADER_MISSING;
import static nus.edu.u.system.enums.ErrorCodeConstants.ROLE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.mapper.role.RoleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    @Mock private RoleMapper roleMapper;
    @InjectMocks private ExcelService excelService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.remove(RoleDO.class);
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, "excelServiceTestMapper");
        assistant.setCurrentNamespace("excelServiceTestNamespace");
        TableInfoHelper.initTableInfo(assistant, RoleDO.class);
    }

    @AfterEach
    void tearDown() {
        TableInfoHelper.remove(RoleDO.class);
    }

    @Test
    void parseCreateOrUpdateRows_nullFile_returnsEmptyList() throws IOException {
        assertTrue(excelService.parseCreateOrUpdateRows(null).isEmpty());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void parseCreateOrUpdateRows_emptyFile_returnsEmptyList() throws IOException {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "empty.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[0]);

        assertTrue(excelService.parseCreateOrUpdateRows(file).isEmpty());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void parseCreateOrUpdateRows_validFile_success() throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of(" email ", "roleKeys", "remark"),
                        List.of(
                                List.of(
                                        " user@example.com ",
                                        "\u00A0admin , manager , manager; user guest",
                                        "\u00A0Remark ")));

        when(roleMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                role(1L, "admin"),
                                role(2L, "manager"),
                                role(3L, "user"),
                                role(4L, "guest")));

        List<CreateUserDTO> result = excelService.parseCreateOrUpdateRows(file);

        assertEquals(1, result.size());
        CreateUserDTO dto = result.get(0);
        assertEquals("user@example.com", dto.getEmail());
        assertEquals(List.of(1L, 2L, 3L, 4L), dto.getRoleIds());
        assertEquals("Remark", dto.getRemark());
        assertEquals(2, dto.getRowIndex());
        verify(roleMapper, times(1)).selectList(any());
    }

    @Test
    void parseCreateOrUpdateRows_missingEmailHeader_throwsServiceException() throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("roleKeys", "remark"),
                        List.of(List.of("admin", "note")));

        ServiceException exception =
                assertThrows(ServiceException.class, () -> excelService.parseCreateOrUpdateRows(file));

        assertEquals(EXCEL_HEADER_MISSING.getCode(), exception.getCode());
    }

    @Test
    void parseCreateOrUpdateRows_missingRoleHeader_throwsServiceException() throws IOException {
        MockMultipartFile file =
                createExcelFile(List.of("email", "remark"), List.of(List.of("user@example.com", "note")));

        ServiceException exception =
                assertThrows(ServiceException.class, () -> excelService.parseCreateOrUpdateRows(file));

        assertEquals(EXCEL_HEADER_MISSING.getCode(), exception.getCode());
    }

    @Test
    void parseCreateOrUpdateRows_blankRoleCell_returnsEmptyRoleIds() throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("email", "roleKeys", "remark"),
                        List.of(List.of("user@example.com", "   ", "something")));

        List<CreateUserDTO> rows = excelService.parseCreateOrUpdateRows(file);

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getRoleIds().isEmpty());
        verifyNoInteractions(roleMapper);
    }

    @Test
    void parseCreateOrUpdateRows_multipleRows_reusesRoleCache() throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("email", "roleKeys"),
                        List.of(
                                List.of("first@example.com", "admin"),
                                List.of("second@example.com", "admin")));

        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "admin")));

        List<CreateUserDTO> rows = excelService.parseCreateOrUpdateRows(file);

        assertEquals(2, rows.size());
        assertEquals(List.of(1L), rows.get(0).getRoleIds());
        assertEquals(List.of(1L), rows.get(1).getRoleIds());
        assertEquals(2, rows.get(0).getRowIndex());
        assertEquals(3, rows.get(1).getRowIndex());
        verify(roleMapper, times(1)).selectList(any());
    }

    @Test
    void parseCreateOrUpdateRows_unknownRole_throwsServiceException() throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("email", "roleKeys"),
                        List.of(List.of("user@example.com", "missingRole")));

        when(roleMapper.selectList(any())).thenReturn(List.of());

        ServiceException exception =
                assertThrows(ServiceException.class, () -> excelService.parseCreateOrUpdateRows(file));

        assertEquals(ROLE_NOT_FOUND.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("missingRole"));
    }

    @Test
    void parseCreateOrUpdateRows_illegalArgumentFromMapper_translatesToFormatError()
            throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("email", "roleKeys"),
                        List.of(List.of("user@example.com", "admin")));

        when(roleMapper.selectList(any())).thenThrow(new IllegalArgumentException("bad arg"));

        ServiceException exception =
                assertThrows(ServiceException.class, () -> excelService.parseCreateOrUpdateRows(file));

        assertEquals(EXCEL_FORMAT_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("bad arg"));
    }

    @Test
    void parseCreateOrUpdateRows_runtimeExceptionFromMapper_translatesToFormatError()
            throws IOException {
        MockMultipartFile file =
                createExcelFile(
                        List.of("email", "roleKeys"),
                        List.of(List.of("user@example.com", "admin")));

        when(roleMapper.selectList(any())).thenThrow(new RuntimeException("boom"));

        ServiceException exception =
                assertThrows(ServiceException.class, () -> excelService.parseCreateOrUpdateRows(file));

        assertEquals(EXCEL_FORMAT_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("Excel parsing failed"));
    }

    private RoleDO role(Long id, String roleKey) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setRoleKey(roleKey);
        return role;
    }

    private MockMultipartFile createExcelFile(List<String> header, List<List<String>> rows)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.size(); i++) {
                headerRow.createCell(i).setCellValue(header.get(i));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = sheet.createRow(r + 1);
                List<String> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    dataRow.createCell(c).setCellValue(values.get(c));
                }
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }
}
