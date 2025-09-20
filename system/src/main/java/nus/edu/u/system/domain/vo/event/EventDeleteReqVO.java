package nus.edu.u.system.domain.vo.event;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class EventDeleteReqVO {
    @NotNull private Long eventId;

    private List<Long> eventIdList;
}
