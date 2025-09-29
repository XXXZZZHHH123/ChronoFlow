package nus.edu.u.system.domain.vo.role;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRespVO {

    private Long id;

    private String name;

    private String key;

    private List<PermissionRespVO> permissions;
}
