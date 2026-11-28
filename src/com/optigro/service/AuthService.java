package com.optigro.service;

import com.optigro.exception.InvalidAuthException;
import com.optigro.models.User;
import com.optigro.repository.UserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthService {
    private final UserRepository userRepo;
    private final Set<String> lockedUsernames = new HashSet<>();

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User login(String username, String password) throws InvalidAuthException {
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidAuthException("Username cannot be empty");
        }

        String cleanUsername = username.trim();

        if (lockedUsernames.contains(cleanUsername)) {
            throw new InvalidAuthException("Security Alert: Account '" + cleanUsername +
                "' is permanently locked after 3 failed attempts. Contact administrator.");
        }

        User user = userRepo.findByUsername(cleanUsername)
            .orElseThrow(() -> new InvalidAuthException("User profile not found: " + cleanUsername));

        if (!user.getPassword().equals(password)) {
            user.incrementFailedAttempts();
            if (user.isLocked()) {
                lockedUsernames.add(cleanUsername);
                throw new InvalidAuthException("Account locked! You exceeded 3 failed password attempts.");
            }
            int remaining = 3 - user.getFailedAttempts();
            throw new InvalidAuthException("Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        user.resetFailedAttempts();
        return user;
    }

    public Set<String> getLockedUsernames() {
        return Collections.unmodifiableSet(lockedUsernames);
    }
}
