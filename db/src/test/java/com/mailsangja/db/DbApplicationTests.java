package com.mailsangja.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DbApplicationTests {

    @Test
    void applicationClassLoads() {
        // given
        Class<DbApplication> applicationClass = DbApplication.class;

        // when & then
        assertNotNull(applicationClass);
    }
}
