package net.nhatanhn.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import net.nhatanhn.models.File;
import net.nhatanhn.repositories.FileRepository;

@Service
public class FileStorageService {
    private final FileRepository repo;
    private final TextStorageClient client;

    public FileStorageService(FileRepository repo, TextStorageClient client) {
        this.repo = repo;
        this.client = client;
    }

    public String read(long id) throws FileNotFoundException, Exception {
        return client.read(id);
    }

    public File save(File f, String content) throws Exception {
        File newFile = new File();
        newFile.setAccessLevel(f.getAccessLevel());
        newFile.setName(f.getName());
        newFile.setType(f.getType());

        File savedFile = repo.save(newFile);
        client.write(savedFile.getId(), content);
        return savedFile;
    }

    public void delete(long id) throws NoSuchFileException, Exception {
        client.delete(id);
        repo.delete(repo.getReferenceById(id));
    }
}

interface TextStorageClient {
    String read(long id) throws Exception;

    void write(long id, String content) throws Exception;

    void delete(long id) throws Exception;
}

// imagine if this was a client for a remote data store like s3 or something
@Component
class LocalStorage implements TextStorageClient {
    private static final String ALGORITHM = "AES";
    private static final SecretKey secret = new SecretKeySpec(
            new byte[] { 'e', 'x', 'a', 'm', 'p', 'l', 'e', 's', 'e', 'c', 'r', 'e', 't', 'k', 'e', 'y' },
            LocalStorage.ALGORITHM);

    @Override
    public String read(long id) throws FileNotFoundException, Exception {
        Path path = Paths.get("").resolve("local_storage").resolve(id + ".txt");
        Cipher cipher = Cipher.getInstance(LocalStorage.ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secret);

        try (var cis = new CipherInputStream(new FileInputStream(path.toString()), cipher)) {
            return new String(cis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public void write(long id, String content) throws Exception {
        Path path = Paths.get("").resolve("local_storage").resolve(id + ".txt");
        Cipher cipher = Cipher.getInstance(LocalStorage.ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secret);

        Files.createDirectories(path.getParent());

        try (var cos = new CipherOutputStream(new FileOutputStream(path.toString()), cipher)) {
            cos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void delete(long id) throws NoSuchFileException, Exception {
        Path path = Paths.get("").resolve("local_storage").resolve(id + ".txt");
        Files.delete(path);
    }
}