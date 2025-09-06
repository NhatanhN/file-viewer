package net.nhatanhn.adapters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

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