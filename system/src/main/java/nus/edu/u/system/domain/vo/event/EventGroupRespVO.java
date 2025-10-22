package nus.edu.u.system.domain.vo.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lu Shuwen
 * @date 2025-10-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventGroupRespVO {

    private Long id;

    private String name;

    private List<Member> members;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Member {

        private Long id;

        private String username;
    }
}
