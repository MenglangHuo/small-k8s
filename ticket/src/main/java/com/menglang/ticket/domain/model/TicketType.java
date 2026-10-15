package com.menglang.ticket.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketType {
    BUG("bug"),
    ENHANCEMENT("enhancement"),
    QUESTION("question"),
    TASK("task");

    private final String value;

    TicketType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return BUG;
        }
        for (TicketType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ticket type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
