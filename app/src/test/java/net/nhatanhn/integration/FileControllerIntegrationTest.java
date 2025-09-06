package net.nhatanhn.integration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.nhatanhn.repositories.AccountRepository;
import net.nhatanhn.repositories.FileRepository;
import net.nhatanhn.services.Auth;
import net.nhatanhn.services.FileStorageService;
import net.nhatanhn.adapters.TextStorageClient;
import net.nhatanhn.models.Account;
import net.nhatanhn.models.File;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FileControllerIntegrationTest {
    @Autowired
    private MockMvc mmvc;
    @Autowired
    private ObjectMapper objMapper;
    @Autowired
    private AccountRepository accRepo;
    @Autowired
    private FileRepository fileRepo;
    @Autowired
    private FileStorageService fileService;

    @Autowired
    @MockitoBean
    private TextStorageClient client;

    private Map<Long, String> inMemoryStorage = new HashMap<>();
    private Account testAdminUser = new Account();
    {
        testAdminUser.setUsername("TEST_ADMIN_USERNAME");
        testAdminUser.setPassword(Auth.hashPassword("TEST_PASSWORD"));
        testAdminUser.setAccessLevel(1000);
        testAdminUser.setIsAdmin(true);
    }

    @BeforeEach
    void mockStorageClient() throws Exception {
        fileRepo.deleteAll();

        when(client.read(anyLong())).thenAnswer(invocation -> {
            if (!inMemoryStorage.containsKey(invocation.getArgument(0))) {
                throw new Exception();
            }
            return inMemoryStorage.get(invocation.getArgument(0));
        });

        doAnswer(invocation -> {
            long id = invocation.getArgument(0);
            String content = invocation.getArgument(1);
            inMemoryStorage.put(id, content);
            return null;
        }).when(client).write(anyLong(), anyString());

        doAnswer(invocation -> {
            long id = invocation.getArgument(0);
            inMemoryStorage.remove(id);
            return null;
        }).when(client).delete(anyLong());

        accRepo.save(testAdminUser);
    }

    @Test
    void shouldUploadFile() throws Exception {
        String json = objMapper.writeValueAsString(Map.of(
                "name", "test_file",
                "type", "text",
                "accessLevel", 1,
                "textContent", "test_content"));

        mmvc.perform(post("/file")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", getSessionToken())
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").exists());
    }

    @Test
    void shouldRetrieveFile() throws Exception {
        long id = setUpTestFile();

        mmvc.perform(get(String.format("/file?fileId=%d", id))
                .header("Authorization", getSessionToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());

    }

    @Test
    void shouldDeleteFile() throws Exception {
        long id = setUpTestFile();

        mmvc.perform(delete(String.format("/file?fileId=%d", id))
                .header("Authorization", getSessionToken()))
                .andExpect(status().isNoContent());
    }

    // v helpers v
    long setUpTestFile() throws Exception {
        File f = new File();
        f.setName("test_file");
        f.setType("text");
        f.setAccessLevel(1);

        File savedFile = fileService.save(f, "test_content");
        return savedFile.getId();
    }

    String getSessionToken() {
        long id = accRepo.findByUsername("TEST_ADMIN_USERNAME").get().getId();
        return Auth.generateToken(id);
    }

}
