package nus.edu.u.common.core.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sorting fields DTO
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortingField implements Serializable {

  /** Order - Ascending */
  public static final String ORDER_ASC = "asc";

  /** Order - Descending */
  public static final String ORDER_DESC = "desc";

  private String field;

  private String order;
}
