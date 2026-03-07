package com.example;

import java.util.List;

/**
 * REST controller for user management.
 */
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public User getUser(String id) {
        return userService.findById(id);
    }

    public List<User> listUsers(int page, int size) {
        return userService.findAll(page, size);
    }

    public User createUser(String username, String email) {
        return userService.create(username, email);
    }

    public void deleteUser(String id) {
        userService.delete(id);
    }
}
