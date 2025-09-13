package nus.edu.u.common.core.domain.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/**
 * Base data object contains common fields
 *
 * @author Lu Shuwen
 * @date 2025-08-27
 */
@Data
public abstract class BaseDO implements Serializable {

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  /**
   * Store user id now
   *
   * <p>Use String type for extension
   */
  @TableField(fill = FieldFill.INSERT, jdbcType = JdbcType.VARCHAR)
  private String creator;

  /**
   * Store user id now
   *
   * <p>Use String type for extension
   */
  @TableField(fill = FieldFill.INSERT_UPDATE, jdbcType = JdbcType.VARCHAR)
  private String updater;

  @TableLogic private Boolean deleted;
}
