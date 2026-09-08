package com.lecturebot.usercourse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserCourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCourseServiceApplication.class, args);
    }
}
