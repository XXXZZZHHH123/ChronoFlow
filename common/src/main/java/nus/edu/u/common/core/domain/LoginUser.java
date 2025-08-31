package nus.edu.u.common.core.domain;

import lombok.Builder;
import lombok.Data;

/**
 * Login User Object
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Data
@Builder
public class LoginUser {

    private Long id;

    private Long roleId;

    private Long tenantId;

    private Long expireTime;
}
