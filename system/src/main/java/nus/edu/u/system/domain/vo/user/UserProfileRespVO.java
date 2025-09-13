package nus.edu.u.system.domain.vo.user;

import lombok.Data;
import java.util.List;

@Data
public class UserProfileRespVO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private List<Long> roles;
    private boolean isRegistered;
}