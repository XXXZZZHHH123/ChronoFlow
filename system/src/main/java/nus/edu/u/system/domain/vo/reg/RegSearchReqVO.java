package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.NumberFormat;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegSearchReqVO {

    @NotNull(message = "Organization id can't be empty")
    private Long organizationId;

    @NotNull(message = "Member id can't be empty")
    private Long userId;
}
