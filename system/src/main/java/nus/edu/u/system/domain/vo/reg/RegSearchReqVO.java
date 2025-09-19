package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegSearchReqVO {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @NotNull(message = "Member ID is required")
    private Long userId;
}
