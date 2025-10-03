package nus.edu.u.system.domain.vo.member;

import java.util.List;
import lombok.Data;

/**
 * Response VO for member profile data on dashboard.
 */
@Data
public class MemberProfileRespVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private Long tenantId;
    private List<RoleVO> roles;

    @Data
    public static class RoleVO {
        private Long id;
        private String name;
        private String key;
    }
}
