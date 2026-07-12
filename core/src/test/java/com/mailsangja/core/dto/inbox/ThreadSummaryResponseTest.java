package com.mailsangja.core.dto.inbox;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadSummaryResponseTest {

    @Test
    void nullableSummaryStringsAreNormalizedToEmptyStrings() {
        ThreadSummaryResponse response = new ThreadSummaryResponse(
                UUID.randomUUID(),
                "gmail-thread-id",
                UUID.randomUUID(),
                null,
                new MailAddressResponse(null, null),
                null,
                false,
                false,
                null,
                List.of(),
                0,
                List.of()
        );

        assertEquals("", response.latestSubject());
        assertEquals("", response.snippet());
        assertEquals("", response.participant().email());
    }
}
