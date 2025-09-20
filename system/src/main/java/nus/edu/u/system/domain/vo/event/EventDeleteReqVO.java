package nus.edu.u.system.domain.vo.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EventDeleteReqVO {
    @NotNull
    private Long eventId;

    private List<Long> eventIdList;
}
