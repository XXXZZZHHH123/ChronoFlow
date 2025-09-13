package nus.edu.u.system.domain.vo.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create group request VO */
@Data
public class CreateGroupReqVO {
  @NotBlank(message = "Group name cannot be empty")
  @Size(max = 50, message = "Group name cannot exceed 50 characters")
  private String name;

  @NotNull(message = "Event ID cannot be null")
  private Long eventId;

  private Long leadUserId;

  private String phone;

  private String email;

  @Size(max = 255, message = "Remark cannot exceed 255 characters")
  private String remark;

  private Integer sort = 0;
}
