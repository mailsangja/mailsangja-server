package com.mailsangja.core.dto.contact;

import com.mailsangja.db.entity.contact.Contact;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "주소록 응답")
public record ContactResponse(
        @Schema(description = "주소록 ID", format = "uuid")
        UUID id,

        @Schema(description = "연락처 이름", example = "Alice")
        String name,

        @Schema(description = "연락처 이메일", example = "alice@example.com")
        String email
) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.getId(), contact.getName(), contact.getEmail());
    }
}
