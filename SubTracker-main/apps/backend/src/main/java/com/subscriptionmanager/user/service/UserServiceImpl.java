package com.subscriptionmanager.user.service;

import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final CurrentUserService currentUserService;

    public UserServiceImpl(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public UserProfileResponse me() {
        User user = currentUserService.requireCurrentUser();
        return new UserProfileResponse(user.getId(), user.getEmail(), "UTC");
    }
}
