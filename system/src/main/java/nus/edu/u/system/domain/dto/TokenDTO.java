package nus.edu.u.system.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Token transform DTO
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Data
@Builder
public class TokenDTO {

    private Long userId;

    private String accessToken;

    private Long expireTime;
}
