package com.example.blogapp.service;

import com.example.blogapp.dto.AuthResponse;
import com.example.blogapp.dto.LoginRequest;
import com.example.blogapp.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
