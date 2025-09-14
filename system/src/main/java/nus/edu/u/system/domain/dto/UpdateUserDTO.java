package nus.edu.u.system.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {
  private Long id;
  private String email;
  private String remark;
  private List<Long> roleIds;
}
