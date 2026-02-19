package com.example.library.security.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.library.jwt.JwtService;
import com.example.library.user.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {

			String token = header.substring(7);

			if (jwtService.isTokenValid(token)) {

				String username = jwtService.extractUsername(token);

				UserDetails user = userDetailsService.loadUserByUsername(username);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
						user.getAuthorities());

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		
		filterChain.doFilter(request, response);
	}
}