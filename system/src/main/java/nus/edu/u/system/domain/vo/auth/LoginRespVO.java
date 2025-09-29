package nus.edu.u.system.domain.vo.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus.edu.u.system.domain.vo.role.RoleRespVO;

import java.util.List;

/** User login response VO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRespVO {

    private UserVO user;

    private List<RoleRespVO> roles;

    @JsonIgnore private String refreshToken;
}
