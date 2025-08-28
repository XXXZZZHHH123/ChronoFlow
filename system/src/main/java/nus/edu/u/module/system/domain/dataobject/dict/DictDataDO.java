package nus.edu.u.module.system.domain.dataobject.dict;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.module.system.domain.dataobject.base.BaseDO;

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
public class DictDataDO extends BaseDO {

    @TableId
    private Long id;

    private Integer sort;

    private String label;

    private String value;

    /**
     * Related to {@link DictTypeDO#getType()}
     */
    private String dictType;

    /**
     * Related to {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;

}
