package nus.edu.u.system.domain.vo.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * User login response VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRespVO {

    private UserVO user;

    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private Long accessTokenExpireTime;

}
