package nus.edu.u.system.service.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExcelService {

    /**
     * 解析 Excel，输出 CreateUserDTO 列表
     * 要求表头包含：email | roleIds | remark
     * roleIds 支持逗号分隔：1,2,3
     */
    public List<CreateUserDTO> parseCreateOrUpdateRows(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        final List<CreateUserDTO> rows = new ArrayList<>();

        EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
            // 记录表头（index -> headerName）
            private final Map<Integer, String> headerIndexMap = new HashMap<>();
            private boolean headerInitialized = false;
            private int currentRowIndex = 0; // 从0开始计；数据第一行是2

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                // 读取表头
                headerIndexMap.clear();
                headMap.forEach((idx, name) -> headerIndexMap.put(idx, safe(name)));
                headerInitialized = true;
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                currentRowIndex++; // 包括表头行
                if (!headerInitialized) {
                    // 有些表没有单独 head 事件时，第一行当作表头
                    headerIndexMap.clear();
                    data.forEach((idx, val) -> headerIndexMap.put(idx, safe(val)));
                    headerInitialized = true;
                    return;
                }

                // 真实数据行（表头下一行），Excel 行号用于回传错误
                int excelRow = currentRowIndex + 1; // EasyExcel内部从0开始；这里约定：Excel第一行为1

                // 找出列索引（不区分大小写）
                Integer emailIdx  = colIndexOf("email");
                Integer rolesIdx  = colIndexOf("roleids");
                Integer remarkIdx = colIndexOf("remark");

                if (emailIdx == null) {
                    // 没有 email 列就跳过
                    log.warn("Row {} skipped: header 'email' not found", excelRow);
                    return;
                }

                String email  = safe(data.get(emailIdx));
                String roles  = rolesIdx == null ? "" : safe(data.get(rolesIdx));
                String remark = remarkIdx == null ? "" : safe(data.get(remarkIdx));

                if (email.isBlank()) {
                    // 空行跳过
                    return;
                }

                List<Long> roleIds = parseRoleIds(roles);

                CreateUserDTO dto = CreateUserDTO.builder()
                        .email(email.trim())
                        .roleIds(roleIds)
                        .remark(remark)
                        .build();
                // 可选：把行号塞进 DTO（如果你在 DTO 里加了这个字段）
                try {
                    var field = CreateUserDTO.class.getDeclaredField("rowIndex");
                    field.setAccessible(true);
                    field.set(dto, excelRow);
                } catch (NoSuchFieldException ignored) {
                    // 你的 DTO 没这个字段就忽略
                } catch (IllegalAccessException e) {
                    log.warn("set rowIndex failed: {}", e.getMessage());
                }

                rows.add(dto);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) { }

            private String safe(String s) { return s == null ? "" : s.trim(); }

            private Integer colIndexOf(String nameLower) {
                for (Map.Entry<Integer, String> e : headerIndexMap.entrySet()) {
                    if (e.getValue() != null && e.getValue().trim().equalsIgnoreCase(nameLower)) {
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
        }).sheet().doRead();

        return rows;
    }

    /**
     * 生成导入模板的表头（如果你想提供“下载模板”的功能，可用该表头写一个空文件）
     */
    public List<String> templateHeaders() {
        return List.of("email", "roleIds", "remark");
    }
}