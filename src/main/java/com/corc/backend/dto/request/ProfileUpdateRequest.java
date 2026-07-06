package com.corc.backend.dto.request;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String name;
    private String email;
    private String phone;
    private String avatar;
}
