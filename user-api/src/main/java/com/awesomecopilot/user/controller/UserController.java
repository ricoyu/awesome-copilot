package com.awesomecopilot.user.controller;

import com.awesomecopilot.common.lang.vo.Result;
import com.awesomecopilot.common.lang.vo.Results;
import com.awesomecopilot.user.dto.UserRequest;
import com.awesomecopilot.user.entity.User;
import com.awesomecopilot.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Management REST API Controller
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Create a new user
     */
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return Results.<User>success()
                .message("User created successfully")
                .data(user)
                .build();
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Results.<User>success()
                .data(user)
                .build();
    }

    /**
     * Get all users
     */
    @GetMapping
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Results.<List<User>>success()
                .data(users)
                .build();
    }

    /**
     * Update user by ID
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        User user = userService.updateUser(id, request);
        return Results.<User>success()
                .message("User updated successfully")
                .data(user)
                .build();
    }

    /**
     * Delete user by ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Results.<Void>success()
                .message("User deleted successfully")
                .build();
    }
}
