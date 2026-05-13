package com.mailsangja.core.service.google;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.config.properties.GooglePeopleProperties;
import com.mailsangja.core.dto.contact.GoogleContactResponse;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GooglePeopleContactQueryService {

    private static final String PERSON_FIELDS = "names,emailAddresses";
    private static final String READ_MASK = "names,emailAddresses";

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
        collectConnections(accessToken, deduplicatedResults);
        collectOtherContacts(accessToken, deduplicatedResults);
        return List.copyOf(deduplicatedResults.values());
    }

    private void collectConnections(String accessToken, Map<String, GoogleContactResult> results) {
        String pageToken = null;
        do {
            GoogleContactResponse response = fetchContactsPage(accessToken, buildConnectionsUri(pageToken));
            addResults(results, toResults(response.connections()));
            pageToken = normalizeBlankToNull(response.nextPageToken());
        } while (pageToken != null);
    }

    private void collectOtherContacts(String accessToken, Map<String, GoogleContactResult> results) {
        String pageToken = null;
        do {
            GoogleContactResponse response = fetchContactsPage(accessToken, buildOtherContactsUri(pageToken));
            addResults(results, toResults(response.otherContacts()));
            pageToken = normalizeBlankToNull(response.nextPageToken());
        } while (pageToken != null);
    }

    private GoogleContactResponse fetchContactsPage(String accessToken, String uri) {
        try {
            GoogleContactResponse response = googlePeopleRestClient
                    .get()
                    .uri(uri)
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
        } catch (RestClientResponseException e) {
            log.warn(
                    "Google People contacts fetch failed. status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw new ContactException(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED);
        } catch (RestClientException e) {
            log.warn("Google People contacts fetch failed.", e);
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

    private String buildOtherContactsUri(String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(googlePeopleProperties.getOtherContactsUri())
                .queryParam("readMask", READ_MASK)
                .queryParam("pageSize", googlePeopleProperties.getPageSize());

        if (!isBlank(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }
        return builder.build(false).toUriString();
    }

    private void addResults(Map<String, GoogleContactResult> saved, List<GoogleContactResult> fetched) {
        for (GoogleContactResult result : fetched) {
            saved.putIfAbsent(result.email(), result);
        }
    }

    private List<GoogleContactResult> toResults(List<GoogleContactResponse.PersonResponse> people) {
        if (people == null || people.isEmpty()) {
            return List.of();
        }

        List<GoogleContactResult> results = new ArrayList<>();
        for (GoogleContactResponse.PersonResponse person : people) {
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

        GoogleContactResponse.NameResponse name = findFirstValidName(person.names());
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
                || isBlank(googlePeopleProperties.getOtherContactsUri())
                || googlePeopleProperties.getPageSize() <= 0) {
            throw new ContactException(ContactErrorCode.GOOGLE_CONTACTS_FETCH_FAILED);
        }
    }

    private GoogleContactResponse.NameResponse findFirstValidName(List<GoogleContactResponse.NameResponse> names) {
        for (GoogleContactResponse.NameResponse name : names) {
            if (name != null && hasNameValue(name)) {
                return name;
            }
        }
        return null;
    }

    private boolean hasNameValue(GoogleContactResponse.NameResponse name) {
        return !isBlank(name.displayName()) || !isBlank(name.givenName()) || !isBlank(name.familyName());
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
