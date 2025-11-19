package com.core.auction_system.dto;

import lombok.Data;

@Data
public class UserProfile {
    private Integer id;
    private String username;
    private String email;
    private String role;
}
