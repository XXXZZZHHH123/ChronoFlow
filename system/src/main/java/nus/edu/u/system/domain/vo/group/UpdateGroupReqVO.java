package nus.edu.u.system.domain.vo.group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Update group request VO */
@Data
public class UpdateGroupReqVO {
    @NotNull(message = "Group ID cannot be null")
    private Long id;

    @Size(max = 50, message = "Group name cannot exceed 50 characters")
    private String name;

    private Long leadUserId;

    private String phone;

    private String email;

    @Size(max = 255, message = "Remark cannot exceed 255 characters")
    private String remark;

    private Integer sort;

    private Integer status;
}
