package nus.edu.u.system.domain.vo.group;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Group response VO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRespVO {
    private Long id;

    private String name;

    private Integer sort;

    private Long leadUserId;

    private String leadUserName;

    private String remark;

    private Integer status;

    private String statusName;

    private Long eventId;

    private String eventName;

    private Integer memberCount;

    private List<MemberInfo> members;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long userId;
        private String username;
        private String email;
        private String phone;
        private Long roleId;
        private String roleName;
        private LocalDateTime joinTime;
    }
}
