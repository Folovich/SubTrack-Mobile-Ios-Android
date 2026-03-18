package com.subscriptionmanager.user.controller;

import com.subscriptionmanager.user.dto.UserProfileResponse;
import com.subscriptionmanager.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.me();
    }
}
