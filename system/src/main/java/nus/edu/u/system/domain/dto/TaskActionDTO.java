package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Lu Shuwen
 * @date 2025-10-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActionDTO {

    private String name;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long targetUserId;

    private List<MultipartFile> files;

    private LocalDateTime eventStartTime;

    private LocalDateTime eventEndTime;
}
