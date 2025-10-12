package com.sorinwork.tickets.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

class AdminLoginTest extends IntegrationTest {

  @Autowired AdminUserRepository users;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void createAdmin() {
    users.save(
        new AdminUser(
            "admin@example.com", "Admin", passwordEncoder.encode("secret-pass"), Instant.now()));
  }

  @Test
  void adminPagesRequireLogin() throws Exception {
    mvc.perform(get("/admin"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/admin/login"));
  }

  @Test
  void loginPageIsPublic() throws Exception {
    mvc.perform(get("/admin/login")).andExpect(status().isOk());
  }

  @Test
  void signsInWithValidCredentials() throws Exception {
    mvc.perform(formLogin("/admin/login").user("Admin@Example.com").password("secret-pass"))
        .andExpect(authenticated().withUsername("admin@example.com"))
        .andExpect(redirectedUrl("/admin"));
  }

  @Test
  void rejectsWrongPassword() throws Exception {
    mvc.perform(formLogin("/admin/login").user("admin@example.com").password("nope"))
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/admin/login?error"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void showsDashboardWhenSignedIn() throws Exception {
    mvc.perform(get("/admin")).andExpect(status().isOk());
  }
}
