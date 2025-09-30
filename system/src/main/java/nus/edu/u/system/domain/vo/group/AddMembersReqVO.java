package nus.edu.u.system.domain.vo.group;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddMembersReqVO {
    @NotEmpty(message = "user ID list can't be empty")
    private List<Long> userIds;
}
