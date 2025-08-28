package nus.edu.u.system.domain.dataobject.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;

/**
 * Tenant basic class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantBaseDO  extends BaseDO {

    /**
     * Related to {@link TenantDO#getId()}
     */
    private Long tenantId;
}
