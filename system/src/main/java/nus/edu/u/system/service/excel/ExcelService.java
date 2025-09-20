package nus.edu.u.system.service.excel;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.*;
import static nus.edu.u.system.enums.ErrorCodeConstants.ROLE_NOT_FOUND;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.mapper.role.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ExcelService {

    @Resource private RoleMapper roleMapper;

    /**
     * Parse Excel -> CreateUserDTO list Requires the header to contain at least: email | roleKeys
     * (remark optional). Supports delimiters for roleKeys: English/Chinese commas, semicolons, and
     * spaces; unknown keys will trigger a friendly exception. Do not skip empty email rows
     * (validation is handled by the business layer).
     */
    public List<CreateUserDTO> parseCreateOrUpdateRows(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Collections.emptyList();
        }

        final List<CreateUserDTO> rows = new ArrayList<>();
        final Map<String, Long> roleKeyCache = new HashMap<>();

        try {
            EasyExcel.read(
                            file.getInputStream(),
                            new AnalysisEventListener<Map<Integer, String>>() {

                                private final Map<Integer, String> headerIndexMap = new HashMap<>();
                                private boolean headerInitialized = false;

                                private Integer emailIdx = null;
                                private Integer rolesIdx = null;
                                private Integer remarkIdx = null;

                                @Override
                                public void invokeHeadMap(
                                        Map<Integer, String> headMap, AnalysisContext context) {
                                    headerIndexMap.clear();
                                    headMap.forEach(
                                            (idx, name) ->
                                                    headerIndexMap.put(idx, normalizeHeader(name)));
                                    headerInitialized = true;

                                    emailIdx = colIndexOf("email");
                                    rolesIdx = colIndexOf("roleKeys");
                                    remarkIdx = colIndexOf("remark");

                                    if (emailIdx == null) {
                                        throw new ServiceException(EXCEL_HEADER_MISSING);
                                    }
                                    if (rolesIdx == null) {
                                        throw new ServiceException(EXCEL_HEADER_MISSING);
                                    }
                                    // remark optional
                                }

                                @Override
                                public void invoke(
                                        Map<Integer, String> data, AnalysisContext context) {
                                    int excelRow =
                                            context.readRowHolder().getRowIndex() + 1; // 1-based

                                    // If the head event is not triggered, treat the first row as
                                    // the table header
                                    if (!headerInitialized) {
                                        headerIndexMap.clear();
                                        data.forEach(
                                                (idx, val) ->
                                                        headerIndexMap.put(
                                                                idx, normalizeHeader(val)));
                                        headerInitialized = true;

                                        emailIdx = colIndexOf("email");
                                        rolesIdx = colIndexOf("roleKeys");
                                        remarkIdx = colIndexOf("remark");

                                        if (emailIdx == null) {
                                            throw new ServiceException(EXCEL_HEADER_MISSING);
                                        }
                                        if (rolesIdx == null) {
                                            throw new ServiceException(EXCEL_HEADER_MISSING);
                                        }
                                        return;
                                    }

                                    String email = getCell(data, emailIdx);
                                    String roles = rolesIdx == null ? "" : getCell(data, rolesIdx);
                                    String remark =
                                            remarkIdx == null ? "" : getCell(data, remarkIdx);

                                    List<String> roleKeys = parseRoleKeysStrict(roles);
                                    List<Long> roleIds =
                                            ExcelService.this.resolveRoleIds(
                                                    roleKeys, excelRow, roleKeyCache);

                                    rows.add(
                                            CreateUserDTO.builder()
                                                    .email(email)
                                                    .roleIds(roleIds)
                                                    .remark(remark)
                                                    .rowIndex(excelRow)
                                                    .build());
                                }

                                @Override
                                public void doAfterAllAnalysed(AnalysisContext context) {
                                    /* no-op */
                                }

                                // ===== helpers =====

                                private String normalizeHeader(String s) {
                                    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
                                }

                                private Integer colIndexOf(String nameLower) {
                                    for (Map.Entry<Integer, String> e : headerIndexMap.entrySet()) {
                                        String v = e.getValue();
                                        if (v != null && v.equalsIgnoreCase(nameLower)) {
                                            return e.getKey();
                                        }
                                    }
                                    return null;
                                }

                                private String getCell(Map<Integer, String> data, Integer idx) {
                                    if (idx == null) return "";
                                    String s = data.get(idx);
                                    if (s == null) return "";
                                    return cleanSpaces(s);
                                }

                                /** Clean NBSP, full-width spaces, and then trim */
                                private String cleanSpaces(String s) {
                                    return s.replace("\u00A0", "") // NBSP
                                            .replace("\u3000", "") // 全角空格
                                            .trim();
                                }

                                /**
                                 * Parse roleKeys. Supports English/Chinese commas, semicolons, and
                                 * spaces. Returns a deduplicated list while preserving appearance
                                 * order.
                                 */
                                private List<String> parseRoleKeysStrict(String cell) {
                                    if (cell == null || cell.isBlank()) {
                                        return Collections.emptyList();
                                    }
                                    Set<String> dedup = new LinkedHashSet<>();
                                    for (String part : cell.split("[,，;\\s]+")) {
                                        if (part == null) continue;
                                        String token = part.trim();
                                        if (!token.isEmpty()) {
                                            dedup.add(token);
                                        }
                                    }
                                    return new ArrayList<>(dedup);
                                }
                            })
                    .sheet()
                    .doRead();
        } catch (ServiceException se) {
            // Direct transparent transmission service friendly exception
            throw se;
        } catch (IllegalArgumentException iae) {
            // Defense: Some underlying IAEs are converted to friendly business exceptions here.
            throw new ServiceException(
                    EXCEL_FORMAT_ERROR.getCode(), "Excel formatting error: " + iae.getMessage());
        } catch (RuntimeException re) {
            // Fallback (other runtime exceptions during parsing phase)
            log.error("Excel parsing failed", re);
            throw new ServiceException(
                    EXCEL_FORMAT_ERROR.getCode(),
                    "Excel parsing failed, please check the template and data format");
        }

        return rows;
    }

    private List<Long> resolveRoleIds(
            List<String> roleKeys, int excelRow, Map<String, Long> roleKeyCache) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keysToQuery =
                roleKeys.stream()
                        .filter(key -> !roleKeyCache.containsKey(key))
                        .collect(Collectors.toList());

        if (!keysToQuery.isEmpty()) {
            List<RoleDO> roles =
                    roleMapper.selectList(
                            new LambdaQueryWrapper<RoleDO>()
                                    .select(RoleDO::getId, RoleDO::getRoleKey)
                                    .in(RoleDO::getRoleKey, keysToQuery));

            Map<String, Long> roleKeyToId =
                    roles.stream()
                            .collect(
                                    Collectors.toMap(
                                            RoleDO::getRoleKey,
                                            RoleDO::getId,
                                            (existing, ignore) -> existing));

            for (String queriedKey : keysToQuery) {
                Long roleId = roleKeyToId.get(queriedKey);
                if (roleId != null) {
                    roleKeyCache.put(queriedKey, roleId);
                }
            }
        }

        List<Long> resolved = new ArrayList<>(roleKeys.size());
        for (String key : roleKeys) {
            Long roleId = roleKeyCache.get(key);
            if (roleId == null) {
                throw new ServiceException(
                        ROLE_NOT_FOUND.getCode(),
                        "Excel row "
                                + excelRow
                                + " column roleKeys contains an unknown roleKey: '"
                                + key
                                + "'");
            }
            resolved.add(roleId);
        }

        return resolved;
    }
}
