package nus.edu.u.system.service.excel;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.*;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ExcelService {

  /**
   * Parse Excel -> CreateUserDTO list Requires the header to contain at least: email | roleIds
   * (remark optional) Supports delimiters for roleIds: English/Chinese commas, semicolons, and
   * spaces; invalid numbers will throw a clear exception. Do not skip empty email rows (validation
   * is handled by the business layer).
   */
  public List<CreateUserDTO> parseCreateOrUpdateRows(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      return Collections.emptyList();
    }

    final List<CreateUserDTO> rows = new ArrayList<>();

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
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                  headerIndexMap.clear();
                  headMap.forEach((idx, name) -> headerIndexMap.put(idx, normalizeHeader(name)));
                  headerInitialized = true;

                  emailIdx = colIndexOf("email");
                  rolesIdx = colIndexOf("roleIds");
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
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                  int excelRow = context.readRowHolder().getRowIndex() + 1; // 1-based

                  // If the head event is not triggered, treat the first row as the table header
                  if (!headerInitialized) {
                    headerIndexMap.clear();
                    data.forEach((idx, val) -> headerIndexMap.put(idx, normalizeHeader(val)));
                    headerInitialized = true;

                    emailIdx = colIndexOf("email");
                    rolesIdx = colIndexOf("roleIds");
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
                  String remark = remarkIdx == null ? "" : getCell(data, remarkIdx);

                  List<Long> roleIds = parseRoleIdsStrict(roles, excelRow);

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
                 * Parse roleIds. Supports English and Chinese commas, semicolons, and spaces.
                 * Explicit exceptions (with line numbers) are thrown for invalid numbers. Returns a
                 * deduplicated list of longs.
                 */
                private List<Long> parseRoleIdsStrict(String cell, int excelRow) {
                  if (cell == null || cell.isBlank()) return Collections.emptyList();
                  List<Long> result = new ArrayList<>();
                  for (String part : cell.split("[,，;\\s]+")) {
                    if (part == null) continue;
                    String token = part.trim();
                    if (token.isEmpty()) continue;
                    try {
                      result.add(Long.valueOf(token));
                    } catch (NumberFormatException ex) {
                      throw new ServiceException(
                          EXCEL_ROLEID_INVALID.getCode(),
                          "Excel row"
                              + excelRow
                              + "column roleIds There is an illegal value: '"
                              + token
                              + "'");
                    }
                  }
                  return result.stream().distinct().collect(Collectors.toList());
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
}
