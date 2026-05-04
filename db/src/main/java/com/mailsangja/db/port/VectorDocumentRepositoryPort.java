package com.mailsangja.db.port;

import java.util.UUID;

public interface VectorDocumentRepositoryPort {

    boolean existsById(UUID messageId);
}
