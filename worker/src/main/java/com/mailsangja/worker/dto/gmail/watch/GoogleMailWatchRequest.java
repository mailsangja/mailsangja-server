package com.mailsangja.worker.dto.gmail.watch;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleMailWatchRequest(
        String topicName,
        List<String> labelIds,
        String labelFilterBehavior
) {
}
