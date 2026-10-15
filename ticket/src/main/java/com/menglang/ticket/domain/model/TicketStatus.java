package com.menglang.ticket.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketStatus {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    RESOLVED("resolved"),
    CLOSED("closed");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        for (TicketStatus status : values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ticket status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
