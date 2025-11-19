package com.core.auction_system.model;

/**
 * Plain POJO used as an in-memory representation of remote user profiles.
 * This is intentionally NOT a JPA entity to avoid creating a local `users` table.
 */
public class User {
    private Integer id;
    private String username;
    private String role;
    private String email;

    public User() {
    }

    public User(Integer id, String username, String role, String email) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
