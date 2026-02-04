package com.befapress.controller;

import com.befapress.dto.request.LoginRequest;
import com.befapress.dto.request.RegisterRequest;
import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.repository.RoleRepository;
import com.befapress.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role intellectualRole;

    @BeforeEach
    void setUp() {
        // Create test role if not exists
        intellectualRole = roleRepository.findByName("ROLE_INTELLECTUAL")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_INTELLECTUAL");
                    role.setDescription("Test intellectual role");
                    return roleRepository.save(role);
                });
    }

    @Test
    void register_ShouldReturnSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("Test@12345");
        request.setAffiliation("Test University");
        request.setExpertiseField("Computer Science");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_WithExistingEmail_ShouldReturnError() throws Exception {
        // Create existing user
        User existingUser = User.builder()
                .fullName("Existing User")
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(intellectualRole)
                .status("ACTIVE")
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("existing@example.com");
        request.setPassword("Test@12345");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnTokens() throws Exception {
        // Create verified user
        User user = User.builder()
                .fullName("Login Test User")
                .email("login@example.com")
                .passwordHash(passwordEncoder.encode("Test@12345"))
                .role(intellectualRole)
                .status("ACTIVE")
                .isEmailVerified(true)
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("Test@12345");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user").exists());
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("invalid-email");
        request.setPassword("Test@12345");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithShortPassword_ShouldReturnValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
