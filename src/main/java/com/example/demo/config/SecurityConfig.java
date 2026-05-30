package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.service.AppUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationFilter jwtAuthenticationFilter,
		DaoAuthenticationProvider authenticationProvider
	) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/",
						"/favicon.ico",
						"/error",
					"/api/auth/**",
					"/api/public/**",
					"/notes-app2.png",
					"/dashboard",
					"/css/**",
					"/js/**",
					"/images/**",
					"/webjars/**",
					"/h2-console/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(form -> form.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN))
			)
			.authenticationProvider(authenticationProvider)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

		return http.build();
	}

	@Bean
	DaoAuthenticationProvider authenticationProvider(AppUserService appUserService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(appUserService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}