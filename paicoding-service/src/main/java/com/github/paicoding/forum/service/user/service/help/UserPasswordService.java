package com.github.paicoding.forum.service.user.service.help;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Unified password operations.
 *
 * <p>Legacy salted MD5 is retained only to verify historical records. Every
 * newly written password is encoded with BCrypt.</p>
 */
@Component
public class UserPasswordService {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private final PasswordEncoder passwordEncoder;
    private final UserPwdEncoder legacyPasswordEncoder;

    public UserPasswordService(PasswordEncoder passwordEncoder, UserPwdEncoder legacyPasswordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            try {
                return passwordEncoder.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return legacyPasswordEncoder.match(rawPassword, storedPassword);
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean isLegacyHash(String storedPassword) {
        return StringUtils.isNotBlank(storedPassword) && !isBcryptHash(storedPassword);
    }

    public boolean isValid(String rawPassword) {
        if (StringUtils.isBlank(rawPassword) || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            return false;
        }

        int categories = 0;
        if (rawPassword.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (rawPassword.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (rawPassword.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (rawPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))) {
            categories++;
        }
        return categories >= 3 && rawPassword.chars().noneMatch(Character::isWhitespace);
    }

    private boolean isBcryptHash(String storedPassword) {
        return storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$");
    }
}
