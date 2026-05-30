package com.example.demo.dto.auth;

public record SendOtpRequest(String email, String username, String password) {
}