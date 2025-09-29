package nus.edu.u.system.domain.dataobject.dict;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.BaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;

/**
 * Dictionary data data object for table sys_dict_data
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName("sys_dict_data")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DictDataDO extends BaseDO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId private Long id;

    private Integer sort;

    private String label;

    private String value;

    /** Related to {@link DictTypeDO#getType()} */
    private String dictType;

    /** Related to {@link CommonStatusEnum} */
    private Integer status;

    private String remark;
}
