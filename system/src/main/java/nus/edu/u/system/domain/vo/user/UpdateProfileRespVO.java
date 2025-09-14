package nus.edu.u.system.domain.vo.user;

import lombok.Data;

@Data
public class UpdateProfileRespVO {
  private Long id;
  private String username;
  private String email;
  private String phone;
  private String remark;
  private Integer status;
}
