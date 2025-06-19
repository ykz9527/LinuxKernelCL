package com.cs.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot应用启动类
 * 
 * @author YK
 * @since 1.0.0
 */
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
        System.out.println("\n" +
                "==========================================================\n" +
                "  应用启动成功! \n" +
                "  应用地址: http://localhost:8080\n" +
                "  API文档: http://localhost:8080/swagger-ui.html\n" +
                "  数据库控制台: http://localhost:8080/h2-console\n" +
                "  健康检查: http://localhost:8080/actuator/health\n" +
                "==========================================================");
    }
} 