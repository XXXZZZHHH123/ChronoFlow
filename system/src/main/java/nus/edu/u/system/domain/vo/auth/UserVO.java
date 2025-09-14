package nus.edu.u.system.domain.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lu Shuwen
 * @date 2025-09-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {

  private Long id;

  private String name;

  private String email;

  private String role;
}
