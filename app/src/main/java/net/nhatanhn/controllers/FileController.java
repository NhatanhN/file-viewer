package net.nhatanhn.controllers;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.nhatanhn.models.File;
import net.nhatanhn.repositories.AccountRepository;
import net.nhatanhn.repositories.FileRepository;
import net.nhatanhn.services.Auth;
import net.nhatanhn.services.FileStorageService;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileStorageService service;
    private final FileRepository fileRepo;
    private final AccountRepository accRepo;

    public FileController(FileStorageService service, FileRepository fileRepo, AccountRepository accRepo) {
        this.service = service;
        this.fileRepo = fileRepo;
        this.accRepo = accRepo;
    }

    public record PostRequest(String name, String type, int accessLevel, String textContent) {
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> get(@RequestHeader("Authorization") String authToken,
            @RequestParam(name = "fileId", defaultValue = "-1") long fileId) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        if (fileId == -1) {
            return ResponseEntity.ok(Map.of("files", fileRepo.findAll().toString()));
        }

        int userAccessLevel = accRepo.findById(userId).get().getAccessLevel();
        int fileAccessLevel;
        try {
            fileAccessLevel = fileRepo.findById(fileId).get().getAccessLevel();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "no file found with given fileId"));
        }

        if (fileAccessLevel > userAccessLevel) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "access level too low"));
        }
        try {
            return ResponseEntity.ok(Map.of("content", service.read(fileId)));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> post(@RequestHeader("Authorization") String authToken,
            @RequestBody PostRequest body) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        int userAccessLevel = accRepo.findById(userId).get().getAccessLevel();
        if (body.accessLevel > userAccessLevel) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "access level too low"));
        }

        if (body.accessLevel == 0 || body.name == null || body.type == null || body.textContent == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "body must contain keys for accessLevel, name, and type"));
        }
        File f = new File();
        f.setAccessLevel(body.accessLevel);
        f.setName(body.name);
        f.setType(body.type);

        try {
            long id = service.save(f, body.textContent).getId();
            return ResponseEntity
                    .created(URI.create("/file"))
                    .body(Map.of("message", "file saved successfully", "fileId", id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("")
    public ResponseEntity<Map<String, Object>> delete(@RequestHeader("Authorization") String authToken,
            @RequestParam("fileId") long fileId) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        int userAccessLevel = accRepo.findById(userId).get().getAccessLevel();
        int fileAccessLevel = fileRepo.findById(fileId).get().getAccessLevel();
        if (fileAccessLevel > userAccessLevel) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "access level too low"));
        }

        try {
            service.delete(fileId);
        } catch (NoSuchFileException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "file not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "failed to delete file"));
        }
        return ResponseEntity.noContent().build();
    }
}
