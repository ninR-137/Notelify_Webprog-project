package com.example.demo.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService implements UserDetailsService {

	private final PasswordEncoder passwordEncoder;
	private final Map<String, StoredUser> usersByEmail = new ConcurrentHashMap<>();

	public AppUserService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public boolean exists(String email) {
		return usersByEmail.containsKey(normalize(email));
	}

	public void register(String email, String username, String rawPassword) {
		String normalizedEmail = normalize(email);
		if (usersByEmail.containsKey(normalizedEmail)) {
			throw new IllegalStateException("An account with this email already exists.");
		}
		usersByEmail.put(normalizedEmail, new StoredUser(
			normalizedEmail,
			username.trim(),
			passwordEncoder.encode(rawPassword)
		));
	}

	public String displayNameFor(String email) {
		StoredUser user = usersByEmail.get(normalize(email));
		return user == null ? "user" : user.username();
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		StoredUser user = usersByEmail.get(normalize(email));
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return User.withUsername(user.email())
			.password(user.passwordHash())
			.roles("USER")
			.build();
	}

	private String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	private record StoredUser(String email, String username, String passwordHash) {
	}
}