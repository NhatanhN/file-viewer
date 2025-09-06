package net.nhatanhn.services;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;

import org.springframework.stereotype.Service;

import net.nhatanhn.adapters.TextStorageClient;
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