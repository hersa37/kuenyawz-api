package dev.realtards.kuenyawz.configurations.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSec) throws Exception {
		httpSec
			.csrf(csrf -> csrf.ignoringRequestMatchers(
				requestMatcher -> requestMatcher.getServletPath().startsWith("/h2-console")
			))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/h2-console/**",				// H2 Console
                    "/api/v1/auth/**",          	// Auth endpoints
                    "/api/v1/pub/**",				// Public endpoints
                    "/swagger-ui/**",				// Swagger UI
                    "/v3/api-docs/**"           	// OpenAPI docs
				).permitAll()
				.anyRequest().authenticated()
			)
			.headers(headers -> headers
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
			)
			.httpBasic(Customizer.withDefaults());

		return httpSec.build();
	}

	@Bean
	public AuthenticationProvider authenticationProvider(AccountUserDetailsService userDetailsService) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
