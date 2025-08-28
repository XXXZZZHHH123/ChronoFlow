package nus.edu.u;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Start application
 *
 * @author Lu Shuwen
 * @date 2025-08-25
 */

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class }, scanBasePackages = "nus.edu.u")
public class ChronoFlowApplication {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void check() {
        if(stringRedisTemplate != null){
            System.out.println("RedisTemplate 注入成功");
        } else {
            System.out.println("RedisTemplate 注入失败");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ChronoFlowApplication.class, args);
    }

}
