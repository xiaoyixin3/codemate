package com.github.paicoding.forum.service.user.service.help;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class UserSessionHelperTest {
    @Test
    void shouldRejectMissingOrWeakJwtSecret() {
        UserSessionHelper.JwtProperties properties = new UserSessionHelper.JwtProperties();
        properties.setIssuer("test");
        properties.setSecret("too-short");
        properties.setExpire(60_000L);

        assertThrows(IllegalStateException.class,
                () -> new UserSessionHelper(properties, mock(RedisTemplate.class)));
    }

    @Test
    void shouldAcceptConfiguredJwtSecret() {
        UserSessionHelper.JwtProperties properties = new UserSessionHelper.JwtProperties();
        properties.setIssuer("test");
        properties.setSecret("test-secret-with-at-least-thirty-two-characters");
        properties.setExpire(60_000L);

        assertDoesNotThrow(() -> new UserSessionHelper(properties, mock(RedisTemplate.class)));
    }
}
