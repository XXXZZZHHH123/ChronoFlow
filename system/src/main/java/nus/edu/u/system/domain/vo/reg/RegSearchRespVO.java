package nus.edu.u.system.domain.vo.reg;

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
public class RegSearchRespVO {

    private String organizationName;

    private String email;

    private String roleName;
}
