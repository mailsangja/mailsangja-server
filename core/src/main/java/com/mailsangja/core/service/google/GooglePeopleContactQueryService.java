package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.config.properties.GooglePeopleProperties;
import com.mailsangja.core.dto.contact.GoogleContactResponse;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GooglePeopleContactQueryService {

    private static final String PERSON_FIELDS = "names,emailAddresses";

    private final GooglePeopleProperties googlePeopleProperties;
    private final RestClient googlePeopleRestClient;

    public GooglePeopleContactQueryService(
            GooglePeopleProperties googlePeopleProperties,
            @Qualifier("googlePeopleRestClient") RestClient googlePeopleRestClient
    ) {
        this.googlePeopleProperties = googlePeopleProperties;
        this.googlePeopleRestClient = googlePeopleRestClient;
    }

    public List<GoogleContactResult> getContacts(String accessToken) {
        validateInput(accessToken);

        Map<String, GoogleContactResult> deduplicatedResults = new LinkedHashMap<>();
        String pageToken = null;
        do {
            GoogleContactResponse response = fetchContactsPage(accessToken, pageToken);
            for (GoogleContactResult result : toResults(response)) {
                deduplicatedResults.putIfAbsent(result.email(), result);
            }
            pageToken = normalizeBlankToNull(response.nextPageToken());
        } while (pageToken != null);

        return List.copyOf(deduplicatedResults.values());
    }

    private GoogleContactResponse fetchContactsPage(String accessToken, String pageToken) {
        try {
            GoogleContactResponse response = googlePeopleRestClient
                    .get()
                    .uri(buildConnectionsUri(pageToken))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleContactResponse.class);

            if (response == null) {
                throw new ContactException(ContactErrorCode.GOOGLE_CONTACTS_RESULT_INVALID);
            }
            return response;
        } catch (ContactException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ContactException(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED);
        }
    }

    private String buildConnectionsUri(String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(googlePeopleProperties.getConnectionsUri())
                .queryParam("personFields", PERSON_FIELDS)
                .queryParam("pageSize", googlePeopleProperties.getPageSize());

        if (!isBlank(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }

        return builder.build(false).toUriString();
    }

    private List<GoogleContactResult> toResults(GoogleContactResponse response) {
        if (response.connections() == null || response.connections().isEmpty()) {
            return List.of();
        }

        List<GoogleContactResult> results = new ArrayList<>();
        for (GoogleContactResponse.PersonResponse person : response.connections()) {
            if (person == null || person.emailAddresses() == null || person.emailAddresses().isEmpty()) {
                continue;
            }

            String name = resolveName(person);
            for (GoogleContactResponse.EmailAddressResponse emailAddress : person.emailAddresses()) {
                if (emailAddress == null || isBlank(emailAddress.value())) {
                    continue;
                }

                String normalizedEmail = normalizeEmail(emailAddress.value());
                String normalizedName = isBlank(name) ? normalizedEmail : name;
                results.add(new GoogleContactResult(normalizedName, normalizedEmail));
            }
        }
        return results;
    }

    private String resolveName(GoogleContactResponse.PersonResponse person) {
        if (person.names() == null || person.names().isEmpty()) {
            return null;
        }

        GoogleContactResponse.NameResponse name = person.names().stream()
                .filter(n -> n != null && (!isBlank(n.displayName()) || !isBlank(n.givenName()) || !isBlank(n.familyName())))
                .findFirst()
                .orElse(null);
        if (name == null) {
            return null;
        }

        if (!isBlank(name.displayName())) {
            return name.displayName().trim();
        }

        String givenName = normalizeBlankToNull(name.givenName());
        String familyName = normalizeBlankToNull(name.familyName());
        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        }
        return givenName != null ? givenName : familyName;
    }

    private void validateInput(String accessToken) {
        if (isBlank(accessToken)
                || isBlank(googlePeopleProperties.getConnectionsUri())
                || googlePeopleProperties.getPageSize() <= 0) {
            throw new ContactException(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeBlankToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
