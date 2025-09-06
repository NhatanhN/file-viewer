package net.nhatanhn.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import net.nhatanhn.models.Account;
import net.nhatanhn.repositories.AccountRepository;
import net.nhatanhn.services.Auth;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mmvc;
    @Autowired
    private AccountRepository accRepo;
    @Autowired
    private ObjectMapper objMapper;

    private static String TEST_ADMIN_USERNAME = "_____test_admin_user";
    private static String TEST_USERNAME = "_____test_user";
    private static String TEST_PASSWORD = "password";

    @BeforeEach
    void setupAdminUser() {
        Account acc = new Account();
        acc.setUsername(TEST_ADMIN_USERNAME);
        acc.setPassword(Auth.hashPassword(TEST_PASSWORD));
        acc.setAccessLevel(1000);
        acc.setIsAdmin(true);
        accRepo.save(acc);
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        String json = objMapper.writeValueAsString(Map.of(
                "username", TEST_ADMIN_USERNAME,
                "password", TEST_PASSWORD));

        mmvc.perform(post("/account/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").exists());
    }

    @Test
    void shouldSignUpSuccessfully() throws Exception {
        String json = objMapper.writeValueAsString(Map.of(
                "username", TEST_USERNAME,
                "password", TEST_PASSWORD,
                "accessLevel", 1));

        mmvc.perform(post("/account/create-new")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", getSessionToken())
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFetchUser() throws Exception {
        setUpTestUser();

        mmvc.perform(get(String.format("/account?username=%s", TEST_USERNAME))
                .header("Authorization", getSessionToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        setUpTestUser();
        long id2 = accRepo.findByUsername(TEST_USERNAME).get().getId();

        mmvc.perform(delete(String.format("/account?id=%d", id2))
                .header("Authorization", getSessionToken()))
                .andExpect(status().isOk());
    }

    // v helpers v
    void setUpTestUser() {
        Account acc = new Account();
        acc.setUsername(TEST_USERNAME);
        acc.setPassword(TEST_PASSWORD);
        acc.setAccessLevel(1);
        accRepo.save(acc);
    }

    String getSessionToken() {
        long id = accRepo.findByUsername(TEST_ADMIN_USERNAME).get().getId();
        return Auth.generateToken(id);
    }
}
