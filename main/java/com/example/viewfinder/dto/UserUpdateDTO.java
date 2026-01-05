package com.example.viewfinder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class UserUpdateDTO {
    
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
    
    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;
    
    private Integer gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    
    @Size(max = 100, message = "所在地不能超过100个字符")
    private String location;
    
    @Size(max = 200, message = "个人网站不能超过200个字符")
    private String website;
    
    private String avatarUrl;
}