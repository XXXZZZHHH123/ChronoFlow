package nus.edu.u.common.core.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Page parameters
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */

@Schema(description="Page parameters")
@Data
public class PageParam implements Serializable {

    private static final Integer PAGE_NO = 1;
    private static final Integer PAGE_SIZE = 10;

    /**
     * Number of records per page, default not page
     *
     * For example, when exporting an interface, you can set {@link #pageSize} to -1 to not paginate and query all data.
     */
    public static final Integer PAGE_SIZE_NONE = -1;

    @Schema(description = "Page No, start from 1", requiredMode = Schema.RequiredMode.REQUIRED,example = "1")
    @NotNull(message = "Page No can't be null")
    @Min(value = 1, message = "Minimum page number is 1")
    private Integer pageNo = PAGE_NO;

    @Schema(description = "Number of entries per page, maximum value is 100", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "Number of records per page can't be null")
    @Min(value = 1, message = "Minimum number of entries per page is 1")
    @Max(value = 100, message = "Maximum number of entries per page is 100")
    private Integer pageSize = PAGE_SIZE;

}
