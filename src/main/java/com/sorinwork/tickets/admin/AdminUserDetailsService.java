package com.sorinwork.tickets.admin;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

  private final AdminUserRepository users;

  public AdminUserDetailsService(AdminUserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    AdminUser user =
        users
            .findByEmail(AdminUser.normalizeEmail(username))
            .orElseThrow(() -> new UsernameNotFoundException("Unknown admin " + username));
    return User.withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .roles("ADMIN")
        .build();
  }
}
