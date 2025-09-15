package nus.edu.u.system.domain.vo.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpsertUsersRespVO {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<RowFailure> failures;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowFailure {
        private int rowIndex;
        private String email;
        private String reason;
    }
}
