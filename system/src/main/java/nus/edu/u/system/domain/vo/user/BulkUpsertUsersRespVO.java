package nus.edu.u.system.domain.vo.user;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpsertUsersRespVO {
    private int totalRows;          // 解析到的有效行数
    private int createdCount;       // 新建条数
    private int updatedCount;       // 更新条数
    private int failedCount;        // 失败条数
    private List<RowFailure> failures; // 失败明细

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowFailure {
        private int rowIndex;       // Excel 中的行号（从 2 开始：1 为表头）
        private String email;
        private String reason;      // 错误码/描述
    }
}
