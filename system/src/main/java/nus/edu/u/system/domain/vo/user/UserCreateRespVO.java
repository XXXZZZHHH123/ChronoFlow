package nus.edu.u.system.domain.vo.user;

import lombok.Data;

@Data
public class UserCreateRespVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
}
