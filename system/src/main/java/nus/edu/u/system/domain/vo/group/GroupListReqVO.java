package nus.edu.u.system.domain.vo.group;

import lombok.Data;

/** Group list request VO */
@Data
public class GroupListReqVO {
  private String name;

  private Long eventId;

  private Integer status;

  private Long leadUserId;

  private Integer pageNo = 1;

  private Integer pageSize = 10;
}
