package com.mailsangja.core.dto.contact;

import java.util.List;

public record GoogleContactResponse(
        List<PersonResponse> connections,
        List<PersonResponse> otherContacts,
        String nextPageToken
) {
    public record PersonResponse(
            List<NameResponse> names,
            List<EmailAddressResponse> emailAddresses
    ) {
    }

    public record NameResponse(
            String displayName,
            String givenName,
            String familyName
    ) {
    }

    public record EmailAddressResponse(
            String value
    ) {
    }
}
