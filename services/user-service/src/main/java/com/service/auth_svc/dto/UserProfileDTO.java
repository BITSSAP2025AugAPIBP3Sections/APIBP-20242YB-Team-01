package com.service.auth_svc.dto;

import com.service.auth_svc.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private boolean enabled;
    private Instant createdAt;
}