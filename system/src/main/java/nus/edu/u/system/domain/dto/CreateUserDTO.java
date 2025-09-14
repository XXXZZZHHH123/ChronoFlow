package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserDTO {
    private String email;
    private List<Long> roleIds;
    private String remark;

    //Excel rowIndex
    private Integer rowIndex;
}
