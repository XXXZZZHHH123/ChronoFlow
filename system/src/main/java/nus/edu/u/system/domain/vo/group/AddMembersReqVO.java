package nus.edu.u.system.domain.vo.group;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AddMembersReqVO {
    @NotEmpty(message = "user ID list can't be empty")
    private List<Long> userIds;
}
