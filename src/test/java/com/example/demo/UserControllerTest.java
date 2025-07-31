package com.example.demo;

import com.example.demo.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserControllerTest {

    @Autowired
    private UserController userController;

    @Test
    void contextLoads() {
        assertThat(userController).isNotNull();
    }
}
