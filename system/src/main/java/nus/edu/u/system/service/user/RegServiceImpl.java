package nus.edu.u.system.service.user;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.vo.reg.RegMemberReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchRespVO;
import nus.edu.u.system.enums.user.UserStatusEnum;
import nus.edu.u.system.mapper.dept.PostMapper;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Service
@Slf4j
public class RegServiceImpl implements RegService {

    @Resource private TenantMapper tenantMapper;

    @Resource private UserMapper userMapper;

    @Resource private PostMapper postMapper;

    @Resource private RoleMapper roleMapper;

    @Resource private UserRoleMapper userRoleMapper;

    @Resource private PasswordEncoder passwordEncoder;

    public static final String ORGANIZER_REMARK = "Organizer account";

    public static final String ORGANIZER_ROLE_NAME = "Organizer";

    public static final String ORGANIZER_ROLE_KEY = "ORGANIZER";

    public static final int ORGANIZATION_CODE_LENGTH = 10;

    public static final int MAX_RETRY_GENERATE_CODE = 5;

    public RegSearchRespVO search(RegSearchReqVO regSearchReqVO) {
        // Select tenant
        TenantDO tenant = tenantMapper.selectById(regSearchReqVO.getOrganizationId());
        if (ObjUtil.isNull(tenant)) {
            throw exception(NO_SEARCH_RESULT);
        }
        // Select user
        UserDO user = userMapper.selectById(regSearchReqVO.getUserId());
        if (ObjUtil.isNull(user)) {
            throw exception(NO_SEARCH_RESULT);
        }
        // Check if user belongs to tenant
        if (!ObjUtil.equals(user.getTenantId(), tenant.getId())) {
            throw exception(NO_SEARCH_RESULT);
        }
        // Check if user has signed up
        if (!ObjUtil.equals(user.getStatus(), UserStatusEnum.PENDING.getCode())) {
            throw exception(ACCOUNT_EXIST);
        }
        // Select position name
        // List<PostDO> postList = postMapper.selectBatchIds(user.getPostList());
        // String post = postList.stream().map(PostDO::getName).collect(Collectors.joining(","));
        // Build return value
        return RegSearchRespVO.builder()
                .organizationName(tenant.getName())
                .email(user.getEmail())
                .build();
    }

    @Override
    public boolean registerAsMember(RegMemberReqVO regMemberReqVO) {
        UserDO user = userMapper.selectById(regMemberReqVO.getUserId());
        if (!ObjUtil.equals(user.getStatus(), UserStatusEnum.PENDING.getCode())) {
            throw exception(ACCOUNT_EXIST);
        }
        user =
                UserDO.builder()
                        .id(regMemberReqVO.getUserId())
                        .username(regMemberReqVO.getUsername())
                        .phone(regMemberReqVO.getPhone())
                        .password(passwordEncoder.encode(regMemberReqVO.getPassword()))
                        .status(UserStatusEnum.ENABLE.getCode())
                        .build();
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional
    public boolean registerAsOrganizer(RegOrganizerReqVO regOrganizerReqVO) {
        // Create and insert user
        UserDO user =
                UserDO.builder()
                        .username(regOrganizerReqVO.getUsername())
                        .email(regOrganizerReqVO.getUserEmail())
                        .phone(regOrganizerReqVO.getMobile())
                        .password(passwordEncoder.encode(regOrganizerReqVO.getUserPassword()))
                        .remark(ORGANIZER_REMARK)
                        .status(UserStatusEnum.ENABLE.getCode())
                        .build();
        boolean isSuccess = userMapper.insert(user) > 0;
        if (!isSuccess) {
            throw exception(REG_FAIL);
        }
        // Insert organizer role
        RoleDO role =
                RoleDO.builder()
                        .name(ORGANIZER_ROLE_NAME)
                        .roleKey(ORGANIZER_ROLE_KEY)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();
        isSuccess = roleMapper.insert(role) > 0;
        if (!isSuccess) {
            throw exception(REG_FAIL);
        }
        // Apply organizer role to this user
        UserRoleDO userRole =
                UserRoleDO.builder().userId(user.getId()).roleId(role.getId()).build();
        isSuccess = userRoleMapper.insert(userRole) > 0;
        if (!isSuccess) {
            throw exception(REG_FAIL);
        }
        // Check tenant code unique
        String code =
                StrUtil.emptyToDefault(
                        regOrganizerReqVO.getOrganizationCode(),
                        RandomUtil.randomString(ORGANIZATION_CODE_LENGTH));
        TenantDO tenantDO;
        int retry = 0;
        while (retry < MAX_RETRY_GENERATE_CODE
                && tenantMapper.exists(
                        new LambdaQueryWrapper<TenantDO>().eq(TenantDO::getTenantCode, code))) {
            code = RandomUtil.randomString(ORGANIZATION_CODE_LENGTH);
            retry++;
        }
        if (retry == MAX_RETRY_GENERATE_CODE) {
            throw exception(EXCEED_MAX_RETRY_GENERATE_CODE);
        }
        // Create and insert tenant
        TenantDO tenant =
                TenantDO.builder()
                        .name(regOrganizerReqVO.getOrganizationName())
                        .contactMobile(regOrganizerReqVO.getMobile())
                        .address(regOrganizerReqVO.getOrganizationAddress())
                        .contactName(regOrganizerReqVO.getName())
                        .contactUserId(user.getId())
                        .tenantCode(code)
                        .build();
        isSuccess = tenantMapper.insert(tenant) > 0;
        if (!isSuccess) {
            throw exception(REG_FAIL);
        }
        // Set tenant id into sys_user and sys_user_role
        user.setTenantId(tenant.getId());
        userMapper.updateById(user);
        role.setTenantId(tenant.getId());
        roleMapper.updateById(role);
        userRole.setTenantId(tenant.getId());
        userRoleMapper.updateById(userRole);
        return isSuccess;
    }
}
