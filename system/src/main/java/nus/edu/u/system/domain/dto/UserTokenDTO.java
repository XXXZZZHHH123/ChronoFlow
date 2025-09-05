package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User data transform object for token
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenDTO {

    private Long id;

    private Long tenantId;

    private Long roleId;

    private boolean remember;
}
