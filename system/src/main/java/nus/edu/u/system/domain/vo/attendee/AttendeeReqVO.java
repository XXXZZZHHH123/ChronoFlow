package nus.edu.u.system.domain.vo.attendee;

import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-10-07
 */
@Data
public class AttendeeReqVO {

    @ExcelProperty("Email")
    @NotEmpty(message = "Email is required")
    private String email;

    @ExcelProperty("Name")
    @NotEmpty(message = "Name is required")
    private String name;

    @ExcelProperty("Mobile")
    @NotEmpty(message = "Mobile is required")
    private String mobile;
}
