package com.mailsangja.db.module.contact;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
@RequiredArgsConstructor
public class ContactBulkInsertModule {

    private static final String TEXT_ARRAY_TYPE = "text";
    private static final String INSERT_ALL_IGNORE_DUPLICATE_ACTIVE_SQL = """
            INSERT INTO contacts (id, user_id, name, email, created_at, modified_at)
            SELECT id, user_id, name, email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM unnest(
                CAST(? AS text[]),
                CAST(? AS text[]),
                CAST(? AS text[]),
                CAST(? AS text[])
            ) AS contact_data(id, user_id, name, email)
            ON CONFLICT (user_id, (lower(email))) WHERE deleted_at IS NULL DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public int insertAllIgnoreDuplicateActive(String[] ids, String[] userIds, String[] names, String[] emails) {
        PreparedStatementCreator creator = connection -> connection.prepareStatement(INSERT_ALL_IGNORE_DUPLICATE_ACTIVE_SQL);
        PreparedStatementCallback<Integer> callback =
                statement -> executeInsertAllIgnoreDuplicateActive(statement, ids, userIds, names, emails);
        return jdbcTemplate.execute(creator, callback);
    }

    private int executeInsertAllIgnoreDuplicateActive(
            PreparedStatement statement,
            String[] ids,
            String[] userIds,
            String[] names,
            String[] emails
    ) throws SQLException {
        statement.setArray(1, createTextArray(statement, ids));
        statement.setArray(2, createTextArray(statement, userIds));
        statement.setArray(3, createTextArray(statement, names));
        statement.setArray(4, createTextArray(statement, emails));
        return statement.executeUpdate();
    }

    private Array createTextArray(PreparedStatement statement, String[] values) throws SQLException {
        return statement.getConnection().createArrayOf(TEXT_ARRAY_TYPE, values);
    }
}
