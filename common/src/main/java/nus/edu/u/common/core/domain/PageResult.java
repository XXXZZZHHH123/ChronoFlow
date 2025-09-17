package nus.edu.u.common.core.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * The return results of page query
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Schema(description = "Page result")
@Data
public final class PageResult<T> implements Serializable {

    @Schema(description = "Data", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> list;

    @Schema(description = "Total", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long total;

    public PageResult() {}

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public PageResult(Long total) {
        this.list = new ArrayList<>();
        this.total = total;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L);
    }

    public static <T> PageResult<T> empty(Long total) {
        return new PageResult<>(total);
    }
}
