package nus.edu.u.system.domain.vo.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BulkUpsertUsersRespVOTest {

    @Test
    void builderCreatesImmutableSnapshot() {
        BulkUpsertUsersRespVO.RowFailure failure =
                BulkUpsertUsersRespVO.RowFailure.builder()
                        .rowIndex(5)
                        .email("duplicate@example.com")
                        .reason("Email already exists")
                        .build();

        BulkUpsertUsersRespVO respVO =
                BulkUpsertUsersRespVO.builder()
                        .totalRows(3)
                        .createdCount(1)
                        .updatedCount(1)
                        .failedCount(1)
                        .failures(List.of(failure))
                        .build();

        assertThat(respVO.getTotalRows()).isEqualTo(3);
        assertThat(respVO.getCreatedCount()).isEqualTo(1);
        assertThat(respVO.getUpdatedCount()).isEqualTo(1);
        assertThat(respVO.getFailedCount()).isEqualTo(1);
        assertThat(respVO.getFailures()).containsExactly(failure);

        assertThat(failure.getRowIndex()).isEqualTo(5);
        assertThat(failure.getEmail()).isEqualTo("duplicate@example.com");
        assertThat(failure.getReason()).isEqualTo("Email already exists");
    }
}
