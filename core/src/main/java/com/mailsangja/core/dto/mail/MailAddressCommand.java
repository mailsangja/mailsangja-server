package com.mailsangja.core.dto.mail;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public record MailAddressCommand(
        String name,
        String address
) {
    public static MailAddressCommand from(MailAddressRequest request) {
        if (request == null) {
            return null;
        }

        return new MailAddressCommand(
                request.name(),
                request.address()
        );
    }

    public static MailAddressCommand fromRaw(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Mail address value is blank.");
        }

        try {
            InternetAddress[] parsedAddresses = InternetAddress.parse(rawValue, true);
            if (parsedAddresses.length != 1 || parsedAddresses[0] == null) {
                throw new IllegalArgumentException("Mail address value must contain exactly one address.");
            }

            InternetAddress parsedAddress = parsedAddresses[0];
            String normalizedAddress = normalizeAddress(parsedAddress.getAddress());
            if (normalizedAddress == null) {
                throw new IllegalArgumentException("Mail address is invalid.");
            }

            String normalizedName = normalizeName(parsedAddress.getPersonal());
            if (normalizedName == null) {
                normalizedName = normalizedAddress;
            }

            return new MailAddressCommand(normalizedName, normalizedAddress);
        } catch (AddressException e) {
            throw new IllegalArgumentException("Mail address format is invalid.", e);
        }
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        return address.trim().toLowerCase();
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String normalizedName = name.replace("\r", " ")
                .replace("\n", " ")
                .trim();
        return normalizedName.isEmpty() ? null : normalizedName;
    }
}
