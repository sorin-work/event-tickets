package com.sorinwork.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

  /**
   * Everything is served by the app itself. Inline handlers are still used for confirm dialogs and
   * htmx attributes, hence 'unsafe-inline' for scripts.
   */
  static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
          + "script-src 'self' 'unsafe-inline'; frame-ancestors 'none'; form-action 'self'; "
          + "base-uri 'self'";

  /**
   * The JSON API. Door endpoints need an admin; scanner apps sign in with HTTP Basic and get a 401
   * rather than the login page.
   */
  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/admin/**").authenticated().anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable());
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/login")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .formLogin(
            form ->
                form.loginPage("/admin/login")
                    .defaultSuccessUrl("/admin", false)
                    .failureUrl("/admin/login?error")
                    .permitAll())
        .logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/admin/login?logout"))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicyHeader(
                        permissions ->
                            permissions.policy("camera=(self), microphone=(), geolocation=()")));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
