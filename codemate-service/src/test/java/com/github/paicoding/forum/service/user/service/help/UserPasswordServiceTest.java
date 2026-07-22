package com.github.paicoding.forum.service.user.service.help;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPasswordServiceTest {
    private UserPwdEncoder legacyPasswordEncoder;
    private UserPasswordService passwordService;

    @BeforeEach
    void setUp() {
        legacyPasswordEncoder = new UserPwdEncoder();
        ReflectionTestUtils.setField(legacyPasswordEncoder, "salt", "legacy-salt");
        ReflectionTestUtils.setField(legacyPasswordEncoder, "saltIndex", 2);
        passwordService = new UserPasswordService(new BCryptPasswordEncoder(4), legacyPasswordEncoder);
    }

    @Test
    void shouldMatchHistoricalSaltedMd5Password() {
        String rawPassword = "Legacy@123";
        String legacyHash = legacyPasswordEncoder.encPwd(rawPassword);

        assertTrue(passwordService.isLegacyHash(legacyHash));
        assertTrue(passwordService.matches(rawPassword, legacyHash));
        assertFalse(passwordService.matches("Legacy@124", legacyHash));
    }

    @Test
    void shouldEncodeAllNewPasswordsWithBcrypt() {
        String rawPassword = "NewPassword@123";
        String bcryptHash = passwordService.encode(rawPassword);

        assertTrue(bcryptHash.startsWith("$2"));
        assertFalse(passwordService.isLegacyHash(bcryptHash));
        assertTrue(passwordService.matches(rawPassword, bcryptHash));
        assertFalse(passwordService.matches("WrongPassword@123", bcryptHash));
    }

    @Test
    void shouldEnforceLengthAndBasicComplexity() {
        assertTrue(passwordService.isValid("ValidPass@123"));
        assertFalse(passwordService.isValid("shrt1A!"));
        assertFalse(passwordService.isValid("onlylowercasepassword"));
        assertFalse(passwordService.isValid("NoSpaces 1!"));
        assertFalse(passwordService.isValid("Aa1!" + new String(new char[61]).replace('\0', 'x')));
    }
}
