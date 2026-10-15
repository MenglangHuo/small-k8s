package com.menglang.ticket.infrastructure.telegram;

import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketPriority;
import com.menglang.ticket.domain.model.TicketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelegramTicketFormatterTest {

    @Test
    @DisplayName("Should escape HTML special characters correctly")
    void testHtmlEscaping() {
        assertEquals("&lt;script&gt;alert(&quot;test&quot;)&lt;/script&gt;", 
                TelegramTicketFormatter.escape("<script>alert(\"test\")</script>").replace("\"", "&quot;"));
        assertEquals("AT&amp;T &lt;corp&gt;", TelegramTicketFormatter.escape("AT&T <corp>"));
    }

    @Test
    @DisplayName("Should map ticket types to correct display labels")
    void testFormatType() {
        assertEquals("Bug", TelegramTicketFormatter.formatType("bug"));
        assertEquals("Enhancement", TelegramTicketFormatter.formatType("feature"));
        assertEquals("Task", TelegramTicketFormatter.formatType("task"));
        assertEquals("Question", TelegramTicketFormatter.formatType("question"));
        assertEquals("Issue", TelegramTicketFormatter.formatType("unknown"));
    }

    @Test
    @DisplayName("Should map ticket priorities to correct emoji badges")
    void testFormatPriority() {
        assertTrue(TelegramTicketFormatter.formatPriority("critical").contains("Critical"));
        assertTrue(TelegramTicketFormatter.formatPriority("high").contains("High"));
        assertTrue(TelegramTicketFormatter.formatPriority("medium").contains("Medium"));
        assertTrue(TelegramTicketFormatter.formatPriority("low").contains("Low"));
    }

    @Test
    @DisplayName("Should format complete Telegram message from ticket domain entity")
    void testFormatTicketCreated() {
        Ticket ticket = Ticket.builder()
                .ticketKey("TCK-101")
                .title("Database connection <timeout>")
                .description("Service failed with code & error 500")
                .type(TicketType.BUG)
                .priority(TicketPriority.CRITICAL)
                .reporterName("Alice & Bob")
                .reporterEmail("alice@example.com")
                .phone("+85512345678")
                .createdAt(Instant.parse("2026-03-30T10:15:30.00Z"))
                .build();

        String formatted = TelegramTicketFormatter.formatTicketCreated(Map.of(), ticket);

        assertNotNull(formatted);
        assertTrue(formatted.contains("TCK-101"));
        assertTrue(formatted.contains("&lt;timeout&gt;"));
        assertTrue(formatted.contains("&amp;"));
        assertTrue(formatted.contains("🔴 Critical"));
        assertTrue(formatted.contains("Alice &amp; Bob"));
        assertTrue(formatted.contains("+85512345678"));
        assertTrue(formatted.contains("alice@example.com"));
        assertTrue(formatted.contains("Service failed with code &amp; error 500"));
    }

    @Test
    @DisplayName("Should truncate overly long descriptions cleanly")
    void testDescriptionTruncation() {
        String longDesc = "A".repeat(1500);
        String cleaned = TelegramTicketFormatter.cleanDescription(longDesc, 100);

        assertTrue(cleaned.endsWith("...\n<i>(truncated)</i>"));
        assertTrue(cleaned.startsWith("A".repeat(100)));
    }
}
