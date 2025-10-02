package nus.edu.u.system.domain.vo.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lu Shuwen
 * @date 2025-09-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRespVO {

    private Long id;

    private String name;

    private String key;

    private String description;
}
