package nus.edu.u.system;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Start application
 *
 * @author Lu Shuwen
 * @date 2025-08-25
 */
@MapperScan(basePackages = "nus.edu.u.system.mapper", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = {"nus.edu.u.framework", "nus.edu.u.common", "nus.edu.u.system", "com.anji.captcha"})
public class ChronoFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChronoFlowApplication.class, args);
    }
}
