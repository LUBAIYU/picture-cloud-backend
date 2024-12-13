package com.by.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author lzh
 */
@SpringBootApplication
@MapperScan("com.by.cloud.mapper")
public class PictureCloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureCloudBackendApplication.class, args);
    }

}
