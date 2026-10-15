package com.menglang.ticket.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketPriority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    private final String value;

    TicketPriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TicketPriority fromValue(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        for (TicketPriority priority : values()) {
            if (priority.value.equalsIgnoreCase(value.trim())) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown ticket priority: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
