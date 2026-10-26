package com.emailforensics.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DotenvLoaderTest {

    @Test
    void testDotenvLoaderExecutesWithoutExceptions() {
        assertDoesNotThrow(() -> DotenvLoader.load());
    }
}
