package nus.edu.u.system.domain.vo.checkin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInReqVO {
    @NotBlank(message = "Check-in token cannot be empty")
    private String token;
}
