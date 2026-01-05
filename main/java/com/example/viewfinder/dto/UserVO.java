package com.example.viewfinder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    private String avatarUrl;
    
    private String nickname;
    
    private String bio;
    
    private Integer gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    
    private String location;
    
    private String website;
    
    private Integer followCount;
    
    private Integer fanCount;
    
    private Integer postCount;
    
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private Boolean isFollowed;
}