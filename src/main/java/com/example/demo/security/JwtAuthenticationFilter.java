package com.example.demo.security;

import java.io.IOException;
import java.util.List;

import com.example.demo.service.AppUserService;
import com.example.demo.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final List<String> PUBLIC_PREFIXES = List.of(
		"/api/auth/",
		"/api/public/",
		"/css/",
		"/js/",
		"/images/",
		"/webjars/",
		"/h2-console/"
	);

	private final JwtService jwtService;
	private final AppUserService appUserService;

	public JwtAuthenticationFilter(JwtService jwtService, AppUserService appUserService) {
		this.jwtService = jwtService;
		this.appUserService = appUserService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			chain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);
		String email;
		try {
			email = jwtService.extractSubjectForType(token, "access");
		} catch (Exception ex) {
			SecurityContextHolder.clearContext();
			chain.doFilter(request, response);
			return;
		}
		if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
			chain.doFilter(request, response);
			return;
		}

		try {
			UserDetails userDetails = appUserService.loadUserByUsername(email);
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
			);
			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authToken);
		} catch (UsernameNotFoundException ex) {
			// Stale token for a user that no longer exists in memory; continue unauthenticated.
			SecurityContextHolder.clearContext();
		}
		chain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null || path.isBlank()) {
			return true;
		}
		if ("/".equals(path) || "/dashboard".equals(path) || "/favicon.ico".equals(path) || "/error".equals(path)) {
			return true;
		}
		return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
	}
}