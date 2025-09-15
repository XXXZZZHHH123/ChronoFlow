package nus.edu.u.system.domain.vo.user;

import java.util.List;
import lombok.Data;

@Data
public class UserProfileRespVO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private List<Long> roles;
    private boolean isRegistered;
}
