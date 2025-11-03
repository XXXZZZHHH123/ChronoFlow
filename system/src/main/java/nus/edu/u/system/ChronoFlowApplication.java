package nus.edu.u.system;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Start application
 *
 * @author Lu Shuwen
 * @date 2025-08-26
 */
@MapperScan(basePackages = "nus.edu.u.system.mapper", annotationClass = Mapper.class)
@SpringBootApplication(
        scanBasePackages = {"nus.edu.u.framework", "nus.edu.u.common", "nus.edu.u.system"})
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
public class ChronoFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChronoFlowApplication.class, args);
    }

    @Bean
    public ApplicationRunner inspectConnections(
            DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        return args -> {
            // ---- 数据库连通性与元信息 ----
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData md = conn.getMetaData();
                log.info("DB URL: {}", md.getURL());
                log.info("DB User: {}", md.getUserName());
                log.info(
                        "DB Product: {} {}",
                        md.getDatabaseProductName(),
                        md.getDatabaseProductVersion());
                log.info("DB Driver: {} {}", md.getDriverName(), md.getDriverVersion());
            } catch (Exception e) {
                log.error("Failed to obtain DB connection or run connectivity check", e);
            }
        };
    }
}
