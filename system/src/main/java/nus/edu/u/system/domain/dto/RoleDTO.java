package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus.edu.u.common.enums.CommonStatusEnum;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {

    private Long id;

    private String name;

    private String roleKey;

    /**
     * Role status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;
}
