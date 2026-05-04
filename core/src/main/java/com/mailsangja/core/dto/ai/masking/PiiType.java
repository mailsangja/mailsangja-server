package com.mailsangja.core.dto.ai.masking;

public enum PiiType {
    EMAIL("EMAIL"),
    PHONE("PHONE"),
    URL("URL"),
    ADDRESS("ADDRESS"),
    ACCOUNT_NUMBER("ACCOUNT"),
    CARD_NUMBER("CARD"),
    KOREAN_RRN("RRN"),
    PERSON_NAME("PERSON");

    private final String tokenPrefix;

    PiiType(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String tokenPrefix() {
        return tokenPrefix;
    }
}
