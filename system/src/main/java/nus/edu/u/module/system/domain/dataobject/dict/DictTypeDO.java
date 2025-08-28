package nus.edu.u.module.system.domain.dataobject.dict;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.core.domain.base.BaseDO;

/**
 * Dictionary type data object for table sys_dict_type
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */

@TableName("sys_dict_type")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictTypeDO extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private String type;

    /**
     * Related to {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;

}
