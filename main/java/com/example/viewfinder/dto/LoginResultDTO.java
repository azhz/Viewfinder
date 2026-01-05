package com.example.viewfinder.dto;

import lombok.Data;

@Data
public class LoginResultDTO {
    private String token;
    private UserVO userInfo;
    
    public LoginResultDTO() {}
    
    public LoginResultDTO(String token, UserVO userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}