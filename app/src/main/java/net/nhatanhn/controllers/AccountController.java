package net.nhatanhn.controllers;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.nhatanhn.models.Account;
import net.nhatanhn.repositories.AccountRepository;
import net.nhatanhn.services.Auth;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/account")
public class AccountController {
    private final AccountRepository accRepo;

    public AccountController(AccountRepository accRepo) {
        this.accRepo = accRepo;
    }

    public record PostBody(String username, String password, int accessLevel, boolean isAdmin) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody PostBody body) {
        if (body.username == null || body.password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "body must contain keys for username and password"));
        }

        Optional<Account> queryResult = accRepo.findByUsername(body.username);
        if (!queryResult.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "invalid username or password"));
        }

        Account account = queryResult.get();
        if (!Auth.verifyPassword(body.password, account.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "invalid username or password"));
        }
        long userId = account.getId();

        String sessionToken = Auth.generateToken(userId);
        return ResponseEntity.ok().body(Map.of("userId", userId, "sessionToken", sessionToken));
    }

    @PostMapping("/create-new")
    public ResponseEntity<Map<String, Object>> createNew(@RequestHeader("Authorization") String authToken,
            @RequestBody PostBody body) {
        // creates the initial admin account,
        // or you could delete this section
        // and create it manually in the 
        // database's account table
        if (accRepo.count() == 0) {
            Account newAccount = new Account();
            newAccount.setUsername(body.username);
            newAccount.setPassword(Auth.hashPassword(body.password));
            newAccount.setAccessLevel(1000);
            newAccount.setIsAdmin(true);

            accRepo.save(newAccount);
            return ResponseEntity.ok().build();
        }

        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        if (body.username == null || body.password == null || body.accessLevel <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "body must contain keys for username, password, and a positive accessLevel"));
        }

        boolean isUserAdmin = accRepo.findById(userId).get().isAdmin();
        if (!isUserAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "only admin users can create new users"));
        }

        if (!accRepo.findByUsername(body.username).isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "username is already taken"));
        }

        Account newAccount = new Account();
        newAccount.setUsername(body.username);
        newAccount.setPassword(Auth.hashPassword(body.password));
        newAccount.setAccessLevel(body.accessLevel);
        newAccount.setIsAdmin(body.isAdmin);

        accRepo.save(newAccount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> get(@RequestHeader("Authorization") String authToken,
            @RequestParam("username") String username) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        Optional<Account> queryResult = accRepo.findByUsername(username);
        if (!queryResult.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(Map.of("userId", queryResult.get().getId()));
    }

    @PatchMapping("")
    public ResponseEntity<Map<String, Object>> update(@RequestHeader("Authorization") String authToken,
            @RequestParam("id") long id,
            @RequestBody PostBody body) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        boolean isUserAdmin = accRepo.findById(userId).get().isAdmin();
        if (!isUserAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "only admin users can change account data"));
        }

        Optional<Account> queryResult = accRepo.findById(id);
        if (!queryResult.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "UserId not found"));
        }

        Account account = queryResult.get();
        if (body.password != null) {
            account.setPassword(Auth.hashPassword(body.password));
        }

        if (body.accessLevel > 0) {
            account.setAccessLevel(body.accessLevel);
        }

        if (body.isAdmin) {
            account.setIsAdmin(body.isAdmin);
        }

        accRepo.save(account);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("")
    public ResponseEntity<Map<String, Object>> delete(@RequestHeader("Authorization") String authToken,
            @RequestParam("id") long id) {
        long userId = Auth.verifyToken(authToken);
        if (userId == -1) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid authentication token"));
        }

        boolean isUserAdmin = accRepo.findById(userId).get().isAdmin();
        if (!isUserAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "only admin users can delete accounts"));
        }

        Optional<Account> queryResult = accRepo.findById(id);
        if (!queryResult.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "UserId not found"));
        }

        Account account = queryResult.get();

        accRepo.delete(account);
        return ResponseEntity.ok().build();
    }
}
