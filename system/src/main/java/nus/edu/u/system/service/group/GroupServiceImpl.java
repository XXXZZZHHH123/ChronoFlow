package nus.edu.u.system.service.group;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupListReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

/**
 * Group service implementation
 * Handles all group-related business logic for event management
 *
 * @author Fan yazhuoting
 * @date 2025-09-09
 */
@Service
@Slf4j
public class GroupServiceImpl implements GroupService{
    @Resource
    private DeptMapper deptMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EventMapper eventMapper;


    @Override
    @Transactional
    public Long createGroup(CreateGroupReqVO reqVO) {
        // 1. Validate event exists
        EventDO event = eventMapper.selectById(reqVO.getEventId());
        if (ObjectUtil.isNull(event)) {
            throw exception(GROUP_NOT_FOUND);
        }

        // 2. Check if group name already exists in this event
        LambdaQueryWrapper<DeptDO> queryWrapper = new LambdaQueryWrapper<DeptDO>()
                .eq(DeptDO::getName, reqVO.getName())
                .eq(DeptDO::getTenantId, event.getTenantId())
                .eq(DeptDO::getStatus, CommonStatusEnum.ENABLE.getStatus());

        DeptDO existingGroup = deptMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotNull(existingGroup)) {
            throw exception(GROUP_NAME_EXISTS);
        }

        // 3. Validate lead user if provided
        if (ObjectUtil.isNotNull(reqVO.getLeadUserId())) {
            UserDO leadUser = userMapper.selectById(reqVO.getLeadUserId());
            if (ObjectUtil.isNull(leadUser)) {
                throw exception(USER_NOT_FOUND);
            }
        }

        // 4. Create group
        DeptDO deptDO = DeptDO.builder()
                .name(reqVO.getName())
                .sort(reqVO.getSort())
                .leadUserId(reqVO.getLeadUserId())
                .phone(reqVO.getPhone())
                .email(reqVO.getEmail())
                .remark(reqVO.getRemark())
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();

        deptMapper.insert(deptDO);

        log.info("Created group: {} with ID: {}", reqVO.getName(), deptDO.getId());
        return deptDO.getId();
    }
}
