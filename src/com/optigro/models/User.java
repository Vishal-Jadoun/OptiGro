package com.optigro.models;

import com.optigro.constants.UserRole;

import java.util.Objects;

public class User {
    private final int userId;
    private final String username;
    private final String password;
    private final UserRole role;
    private int failedAttempts;
    private boolean locked;

    public User(int userId, String username, String password, UserRole role) {
        this.userId = userId;
        this.username = Objects.requireNonNull(username, "Username required").trim();
        this.password = Objects.requireNonNull(password, "Password required");
        this.role = Objects.requireNonNull(role, "Role required");
        this.failedAttempts = 0;
        this.locked = false;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserRole getRole() { return role; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        if (this.failedAttempts >= 3) {
            this.locked = true;
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void unlock() {
        this.locked = false;
        this.failedAttempts = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return String.format("User #%d: %-12s | Role: %-16s | Locked: %b",
            userId, username, role, locked);
    }
}
