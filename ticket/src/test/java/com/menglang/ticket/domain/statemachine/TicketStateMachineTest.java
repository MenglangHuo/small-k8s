package com.menglang.ticket.domain.statemachine;

import com.menglang.ticket.domain.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketStateMachineTest {

    @Test
    @DisplayName("Should allow valid transitions from OPEN")
    void testValidTransitionsFromOpen() {
        assertTrue(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.OPEN));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.RESOLVED));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.CLOSED));
    }

    @Test
    @DisplayName("Should allow valid transitions from IN_PROGRESS")
    void testValidTransitionsFromInProgress() {
        assertTrue(TicketStateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.OPEN));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
    }

    @Test
    @DisplayName("Should allow valid transitions from RESOLVED")
    void testValidTransitionsFromResolved() {
        assertTrue(TicketStateMachine.canTransition(TicketStatus.RESOLVED, TicketStatus.OPEN));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS));
        assertTrue(TicketStateMachine.canTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED));
    }

    @Test
    @DisplayName("Should only allow reopening from CLOSED")
    void testValidTransitionsFromClosed() {
        assertTrue(TicketStateMachine.canTransition(TicketStatus.CLOSED, TicketStatus.OPEN));
        assertFalse(TicketStateMachine.canTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));
        assertFalse(TicketStateMachine.canTransition(TicketStatus.CLOSED, TicketStatus.RESOLVED));
    }

    @Test
    @DisplayName("assertTransition throws IllegalArgumentException on invalid transition")
    void testAssertTransitionThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                TicketStateMachine.assertTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS)
        );

        assertDoesNotThrow(() ->
                TicketStateMachine.assertTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)
        );
    }

    @Test
    @DisplayName("Null transitions should return false")
    void testNullTransitions() {
        assertFalse(TicketStateMachine.canTransition(null, TicketStatus.OPEN));
        assertFalse(TicketStateMachine.canTransition(TicketStatus.OPEN, null));
        assertFalse(TicketStateMachine.canTransition(null, null));
    }
}
