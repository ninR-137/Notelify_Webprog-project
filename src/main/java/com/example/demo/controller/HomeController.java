package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String landingPage() {
		return "landingpage";
	}

	@GetMapping("/dashboard")
	public String dashboardPage() {
		return "dashboard";
	}

	@GetMapping("/favicon.ico")
	public ResponseEntity<Void> favicon() {
		return ResponseEntity.noContent().build();
	}
}