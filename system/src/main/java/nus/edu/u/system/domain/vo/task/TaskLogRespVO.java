package nus.edu.u.system.domain.vo.task;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import nus.edu.u.system.domain.vo.auth.UserVO;
import nus.edu.u.system.domain.vo.file.FileResultVO;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@Data
@Builder
public class TaskLogRespVO {

    private Long id;

    private Integer action;

    private UserVO targetUser;

    private UserVO sourceUser;

    private LocalDateTime createTime;

    private List<FileResultVO> fileResults;
}
