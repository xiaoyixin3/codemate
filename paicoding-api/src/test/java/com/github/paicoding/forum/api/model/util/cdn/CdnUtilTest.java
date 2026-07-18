package com.github.paicoding.forum.api.model.util.cdn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CdnUtilTest {
    @Test
    void shouldKeepNullImageUrl() {
        assertNull(CdnUtil.autoTransCdn(null));
    }

    @Test
    void shouldReplaceLegacyCdnHost() {
        assertEquals("https://cdn.paicoding.com/a.png",
                CdnUtil.autoTransCdn("https://cdn.tobebetterjavaer.com/a.png"));
    }
}
