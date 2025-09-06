package net.nhatanhn.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import net.nhatanhn.adapters.TextStorageClient;
import net.nhatanhn.models.File;
import net.nhatanhn.repositories.FileRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileService;
    @Autowired
    private FileRepository fileRepo;
    @Autowired
    @MockitoBean
    private TextStorageClient client;

    private Map<Long, String> inMemoryStorage = new HashMap<>();

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
    }

    @Test
    void shouldReadFile() throws Exception {
        long id = setUpFile();

        assertEquals(fileService.read(id), "test_content");
    }

    @Test
    void shouldWriteFile() throws Exception {
        File newFile = new File();
        newFile.setAccessLevel(1);
        newFile.setName("test");
        newFile.setType("text");
        String content = "test_content";

        fileService.save(newFile, content);
        assertTrue(fileRepo.count() > 0);
    }

    @Test
    void shouldDeleteFile() throws Exception {
        long id = setUpFile();

        fileService.delete(id);
        assertTrue(fileRepo.count() == 0);
    }

    long setUpFile() throws Exception {
        File newFile = new File();
        newFile.setAccessLevel(1);
        newFile.setName("test");
        newFile.setType("text");

        File savedFile = fileRepo.save(newFile);
        long id = savedFile.getId();
        client.write(id, "test_content");
        return id;
    }
}
