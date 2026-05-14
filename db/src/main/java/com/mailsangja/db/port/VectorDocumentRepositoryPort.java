package com.mailsangja.db.port;

import java.util.UUID;

public interface VectorDocumentRepositoryPort {

    boolean existsByDocumentId(UUID documentId);
}
