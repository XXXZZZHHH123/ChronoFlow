package nus.edu.u.system.domain.vo.user;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRespVO {
    private Long id;

    private String email;

    private String remark;

    private List<Long> roleIds;

    private Integer status;
}
