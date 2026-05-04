package com.mailsangja.db.adapter.vector;

import com.mailsangja.db.port.VectorDocumentRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.regex.Pattern;

@Repository
public class VectorDocumentRepositoryAdapter implements VectorDocumentRepositoryPort {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public VectorDocumentRepositoryAdapter(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = validateTableName(tableName);
    }

    @Override
    public boolean existsById(UUID messageId) {
        Boolean exists = jdbcTemplate.queryForObject(existsSql(), Boolean.class, messageId);
        return Boolean.TRUE.equals(exists);
    }

    private String existsSql() {
        return "SELECT EXISTS (SELECT 1 FROM " + tableName + " WHERE id = ?::uuid)";
    }

    private String validateTableName(String tableName) {
        if (tableName == null || !SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid pgvector table name: " + tableName);
        }
        return tableName;
    }
}
