package com.subscriptionmanager.mapper;

import com.subscriptionmanager.dto.UserResponse;
import com.subscriptionmanager.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
