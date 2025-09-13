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
public class OrganizerUpdateUserDTO {
    private Long id;
    private String email;
    private String remark;
    private List<Long> roleIds;
}
