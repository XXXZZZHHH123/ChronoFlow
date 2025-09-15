package nus.edu.u.system.service.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ExcelService {

    /**
     * Parse Excel and output a CreateUserDTO list Requires the table header to contain: email |
     * roleIds | remark RoleIds supports comma separation: 1, 2, 3
     */
    public List<CreateUserDTO> parseCreateOrUpdateRows(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        final List<CreateUserDTO> rows = new ArrayList<>();

        EasyExcel.read(
                        file.getInputStream(),
                        new AnalysisEventListener<Map<Integer, String>>() {
                            // Record header (index -> headerName)
                            private final Map<Integer, String> headerIndexMap = new HashMap<>();
                            private boolean headerInitialized = false;
                            private int currentRowIndex =
                                    0; // Start counting from 0; the first row of data is 2

                            @Override
                            public void invokeHeadMap(
                                    Map<Integer, String> headMap, AnalysisContext context) {

                                headerIndexMap.clear();
                                headMap.forEach((idx, name) -> headerIndexMap.put(idx, safe(name)));
                                headerInitialized = true;
                            }

                            @Override
                            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                                currentRowIndex++; // Include header row
                                if (!headerInitialized) {
                                    // When some tables do not have a separate head event, the first
                                    // row is used as
                                    // the table header
                                    headerIndexMap.clear();
                                    data.forEach((idx, val) -> headerIndexMap.put(idx, safe(val)));
                                    headerInitialized = true;
                                    return;
                                }

                                // Actual data row (the row below the header), Excel row number is
                                // used to return
                                // errors
                                int excelRow =
                                        currentRowIndex
                                                + 1; // EasyExcel starts from 0 internally; here it
                                // is agreed that the first
                                // line of Excel is 1

                                // Find out the column index (case-insensitive)）
                                Integer emailIdx = colIndexOf("email");
                                Integer rolesIdx = colIndexOf("roleIds");
                                Integer remarkIdx = colIndexOf("remark");

                                if (emailIdx == null) {
                                    // Skip if there is no email column
                                    log.warn("Row {} skipped: header 'email' not found", excelRow);
                                    return;
                                }

                                String email = safe(data.get(emailIdx));
                                String roles = rolesIdx == null ? "" : safe(data.get(rolesIdx));
                                String remark = remarkIdx == null ? "" : safe(data.get(remarkIdx));

                                if (email.isBlank()) {
                                    // Skip blank lines
                                    return;
                                }

                                List<Long> roleIds = parseRoleIds(roles);

                                CreateUserDTO dto =
                                        CreateUserDTO.builder()
                                                .email(email.trim())
                                                .roleIds(roleIds)
                                                .remark(remark)
                                                .rowIndex(excelRow)
                                                .build();

                                rows.add(dto);
                            }

                            @Override
                            public void doAfterAllAnalysed(AnalysisContext context) {}

                            private String safe(String s) {
                                return s == null ? "" : s.trim();
                            }

                            private Integer colIndexOf(String nameLower) {
                                for (Map.Entry<Integer, String> e : headerIndexMap.entrySet()) {
                                    if (e.getValue() != null
                                            && e.getValue().trim().equalsIgnoreCase(nameLower)) {
                                        return e.getKey();
                                    }
                                }
                                return null;
                            }

                            private List<Long> parseRoleIds(String cell) {
                                if (cell == null || cell.isBlank()) return Collections.emptyList();
                                return Arrays.stream(cell.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .map(Long::valueOf)
                                        .distinct()
                                        .collect(Collectors.toList());
                            }
                        })
                .sheet()
                .doRead();

        return rows;
    }
}
