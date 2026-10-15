package com.menglang.ticket.domain.statemachine;

import com.menglang.ticket.domain.model.TicketStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class TicketStateMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        TRANSITIONS.put(TicketStatus.OPEN, EnumSet.of(
                TicketStatus.IN_PROGRESS,
                TicketStatus.RESOLVED,
                TicketStatus.CLOSED
        ));

        TRANSITIONS.put(TicketStatus.IN_PROGRESS, EnumSet.of(
                TicketStatus.OPEN,
                TicketStatus.RESOLVED,
                TicketStatus.CLOSED
        ));

        TRANSITIONS.put(TicketStatus.RESOLVED, EnumSet.of(
                TicketStatus.OPEN,
                TicketStatus.IN_PROGRESS,
                TicketStatus.CLOSED
        ));

        TRANSITIONS.put(TicketStatus.CLOSED, EnumSet.of(
                TicketStatus.OPEN
        ));
    }

    public static boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<TicketStatus> allowed = TRANSITIONS.getOrDefault(from, Collections.emptySet());
        return allowed.contains(to);
    }

    public static void assertTransition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalArgumentException(
                    String.format("Invalid transition from status '%s' to '%s'.",
                            from != null ? from.getValue() : "null",
                            to != null ? to.getValue() : "null")
            );
        }
    }
}
