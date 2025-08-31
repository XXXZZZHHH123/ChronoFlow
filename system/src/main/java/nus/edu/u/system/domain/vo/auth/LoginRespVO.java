package nus.edu.u.system.domain.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User login response VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRespVO {

    private Long userId;

    private String accessToken;

    private String refreshToken;

    private Long expireTime;

}
