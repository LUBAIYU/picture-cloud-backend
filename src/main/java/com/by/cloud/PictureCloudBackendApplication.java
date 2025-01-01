package com.by.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author lzh
 */
@SpringBootApplication
@MapperScan("com.by.cloud.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class PictureCloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureCloudBackendApplication.class, args);
    }

}
