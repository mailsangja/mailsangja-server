package com.mailsangja.db.adapter.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorDocumentRepositoryAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void existsById_returnsTrueWhenVectorRowExists() {
        UUID messageId = UUID.randomUUID();
        VectorDocumentRepositoryAdapter adapter = createAdapter("vector_store");
        when(jdbcTemplate.queryForObject(contains("vector_store"), eq(Boolean.class), eq(messageId)))
                .thenReturn(true);

        boolean exists = adapter.existsById(messageId);

        assertTrue(exists);
    }

    @Test
    void existsById_returnsFalseWhenVectorRowDoesNotExist() {
        UUID messageId = UUID.randomUUID();
        VectorDocumentRepositoryAdapter adapter = createAdapter("vector_store");
        when(jdbcTemplate.queryForObject(contains("vector_store"), eq(Boolean.class), eq(messageId)))
                .thenReturn(false);

        boolean exists = adapter.existsById(messageId);

        assertFalse(exists);
    }

    @Test
    void constructor_rejectsUnsafeTableName() {
        assertThrows(IllegalArgumentException.class, () -> createAdapter("vector_store;drop table messages"));
    }

    private VectorDocumentRepositoryAdapter createAdapter(String tableName) {
        return new VectorDocumentRepositoryAdapter(jdbcTemplate, tableName);
    }
}
