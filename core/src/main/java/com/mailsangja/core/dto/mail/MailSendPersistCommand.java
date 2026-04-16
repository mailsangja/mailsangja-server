package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MailSendPersistCommand(
        MailAccount mailAccount,
        GoogleMailMessageResult messageResult,
        MailSendCommand sendCommand
) {

    public Message.CreateValues toCreateValues() {
        return new Message.CreateValues(
                messageResult.gmailMessageId(),
                Direction.OUTBOUND,
                messageResult.subject(),
                messageResult.fromAddress(),
                resolveFromName(),
                messageResult.toAddresses(),
                resolveNames(messageResult.toAddresses(), sendCommand.to(), messageResult.toNames()),
                messageResult.ccAddresses(),
                resolveNames(messageResult.ccAddresses(), sendCommand.cc(), messageResult.ccNames()),
                messageResult.snippet(),
                true,
                messageResult.sentAt(),
                messageResult.bodyText(),
                messageResult.bodyHtml()
        );
    }

    public String latestParticipantAddress() {
        if (messageResult.toAddresses() != null && !messageResult.toAddresses().isEmpty()) {
            return messageResult.toAddresses().getFirst();
        }

        if (messageResult.ccAddresses() != null && !messageResult.ccAddresses().isEmpty()) {
            return messageResult.ccAddresses().getFirst();
        }

        return null;
    }

    private String resolveFromName() {
        if (sendCommand.from() != null
                && sendCommand.from().address() != null
                && sendCommand.from().address().equals(messageResult.fromAddress())) {
            return sendCommand.from().name();
        }
        return messageResult.fromName();
    }

    private List<String> resolveNames(
            List<String> addresses,
            List<MailAddressCommand> sentAddresses,
            List<String> fallbackNames
    ) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }

        if (sentAddresses == null || sentAddresses.isEmpty()) {
            return fallbackNames == null ? List.of() : fallbackNames;
        }

        Map<String, String> nameByAddress = sentAddresses.stream()
                .collect(Collectors.toMap(
                        MailAddressCommand::address,
                        MailAddressCommand::name,
                        (left, right) -> left
                ));

        return addresses.stream()
                .map(address -> nameByAddress.getOrDefault(address, resolveFallbackName(address, addresses, fallbackNames)))
                .toList();
    }

    private String resolveFallbackName(String address, List<String> addresses, List<String> fallbackNames) {
        if (fallbackNames == null || fallbackNames.isEmpty()) {
            return address;
        }

        int index = addresses.indexOf(address);
        if (index < 0 || index >= fallbackNames.size()) {
            return address;
        }

        String fallbackName = fallbackNames.get(index);
        return fallbackName == null || fallbackName.isBlank() ? address : fallbackName;
    }
}
