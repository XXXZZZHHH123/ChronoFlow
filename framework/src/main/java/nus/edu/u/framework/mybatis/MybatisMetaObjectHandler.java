package nus.edu.u.framework.mybatis;

import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * Config to fill common properties automatically
 *
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        // Fill time automatically
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // Fill user automatically
        String currentUser = getCurrentUserId();
        this.strictInsertFill(metaObject, "creator", String.class, currentUser);
        this.strictInsertFill(metaObject, "updater", String.class, currentUser);

        this.strictInsertFill(metaObject, "tenant_id", Long.class, getCurrentTenantId());
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updater", String.class, getCurrentUserId());
    }

    private String getCurrentUserId() {
        // Get login user id
        try {
            Object userIdObject = StpUtil.getLoginId();
            if (ObjectUtil.isNotNull(userIdObject)) {
                return String.valueOf(userIdObject);
            }
        } catch (Exception ignored) {
        }
        return "system"; // Default value
    }

    private Long getCurrentTenantId() {
        try {
            Object tenantIdObject = StpUtil.getSession().get(SESSION_TENANT_ID);
            Long tenantId = Long.parseLong(tenantIdObject.toString());
            if (ObjectUtil.isNotNull(tenantId)) {
                return tenantId;
            }
        } catch (Exception ignored) {
        }
        return 1L; // Default value
    }
}
