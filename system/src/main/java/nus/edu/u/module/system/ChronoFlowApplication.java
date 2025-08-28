package nus.edu.u.module.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Start application
 *
 * @author Lu Shuwen
 * @date 2025-08-25
 */

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class }, scanBasePackages = "nus.edu.u.module")
public class ChronoFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoFlowApplication.class, args);
    }

}
