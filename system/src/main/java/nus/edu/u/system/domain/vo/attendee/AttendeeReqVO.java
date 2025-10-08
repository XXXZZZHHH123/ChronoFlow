package nus.edu.u.system.domain.vo.attendee;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-10-07
 */
@Data
public class AttendeeReqVO {

    @NotEmpty(message = "Email is required")
    private String email;

    @NotEmpty(message = "Name is required")
    private String name;

    @NotEmpty(message = "Mobile is required")
    private String mobile;
}
