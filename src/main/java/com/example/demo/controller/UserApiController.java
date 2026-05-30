package com.example.demo.controller;

import com.example.demo.dto.user.UserProfileResponse;
import com.example.demo.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserApiController {

	private final AppUserService appUserService;

	public UserApiController(AppUserService appUserService) {
		this.appUserService = appUserService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
		String email = authentication.getName();
		return ResponseEntity.ok(new UserProfileResponse(email, appUserService.displayNameFor(email)));
	}
}