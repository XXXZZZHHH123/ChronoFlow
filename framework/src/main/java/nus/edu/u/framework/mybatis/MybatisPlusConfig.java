package nus.edu.u.framework.mybatis;

import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Lu Shuwen
 * @date 2025-09-12
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // ✅ Muti-tenant
        interceptor.addInnerInterceptor(
                new TenantLineInnerInterceptor(
                        new TenantLineHandler() {
                            @Override
                            public Expression getTenantId() {
                                Long tenantId = getCurrentTenantId();
                                return new LongValue(tenantId);
                            }

                            @Override
                            public String getTenantIdColumn() {
                                return "tenant_id"; // Tenant field name in the database table
                            }

                            @Override
                            public boolean ignoreTable(String tableName) {
                                // Return true to ignore the table and not join the tenant
                                // conditions.
                                return "sys_dict_data".equals(tableName)
                                        || "sys_dict_type".equals(tableName)
                                        || "sys_tenant".equals(tableName);
                            }
                        }));

        // ✅ Pagination
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        // ✅ Avoid update or delete all data in table
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    public static Long getCurrentTenantId() {
        try {
            Object tenantIdObject = StpUtil.getSession().get(SESSION_TENANT_ID);
            Long tenantId = Long.parseLong(tenantIdObject.toString());
            if (ObjectUtil.isNotNull(tenantId)) {
                return tenantId;
            }
        } catch (Exception ignored) {
        }
        return 1L;
    }
}
