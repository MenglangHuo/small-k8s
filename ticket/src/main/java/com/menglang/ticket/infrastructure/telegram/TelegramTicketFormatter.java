package com.menglang.ticket.infrastructure.telegram;

import com.menglang.ticket.domain.model.Ticket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramTicketFormatter {

    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yy - h:mm:ss a")
            .withZone(ZoneId.of("Asia/Phnom_Penh"));

    public static String formatTicketCreated(Map<String, Object> payload, Ticket ticket) {
        String ticketKey = getString(payload, "ticket_key", ticket != null ? ticket.getTicketKey() : "N/A");
        String title = getString(payload, "title", ticket != null ? ticket.getTitle() : "No title");
        String rawDescription = getString(payload, "description", ticket != null ? ticket.getDescription() : "");
        String type = getString(payload, "type", ticket != null && ticket.getType() != null ? ticket.getType().getValue() : "bug");
        String priority = getString(payload, "priority", ticket != null && ticket.getPriority() != null ? ticket.getPriority().getValue() : "medium");
        String reporterName = getString(payload, "reporter_name", ticket != null ? ticket.getReporterName() : null);
        String reporterPhone = getString(payload, "reporter_phone", ticket != null ? ticket.getPhone() : null);
        String reporterEmail = getString(payload, "reporter_email", ticket != null ? ticket.getReporterEmail() : null);
        Instant createdAt = ticket != null && ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now();

        String escapedKey = escape(ticketKey);
        String escapedTitle = escape(title);
        String typeBadge = formatType(type);
        String priorityBadge = formatPriority(priority);
        String formattedDate = DATE_FORMATTER.format(createdAt);
        String cleanDesc = cleanDescription(rawDescription, MAX_DESCRIPTION_LENGTH);

        List<String> lines = new ArrayList<>();
        lines.add("🎫 <b>NEW TICKET ISSUED</b>");
        lines.add("");
        lines.add(String.format("<b>Ticket:</b> %s", escapedKey));
        lines.add(String.format("<b>Title:</b> %s", escapedTitle));
        lines.add(String.format("<b>Type:</b> %s", typeBadge));
        lines.add(String.format("<b>Priority:</b> %s", priorityBadge));
        lines.add(String.format("<b>Created At:</b> %s", formattedDate));

        String reporterSection = formatReporterSection(reporterName, reporterEmail, reporterPhone);
        if (!reporterSection.isBlank()) {
            lines.add("");
            lines.add(reporterSection);
        }

        lines.add("");
        lines.add("<b>Description:</b>");
        lines.add(cleanDesc);

        return String.join("\n", lines);
    }

    public static String formatType(String type) {
        if (type == null) return "Bug";
        return switch (type.toLowerCase().trim()) {
            case "bug" -> "Bug";
            case "enhancement", "feature" -> "Enhancement";
            case "task" -> "Task";
            case "question" -> "Question";
            default -> "Issue";
        };
    }

    public static String formatPriority(String priority) {
        if (priority == null) return "🟡 Medium";
        return switch (priority.toLowerCase().trim()) {
            case "critical", "urgent" -> "🔴 Critical";
            case "high" -> "🟠 High";
            case "medium" -> "🟡 Medium";
            case "low" -> "🟢 Low";
            default -> "⚪ Normal";
        };
    }

    public static String formatReporterSection(String name, String email, String phone) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();

        if (!hasName && !hasEmail && !hasPhone) return "";

        List<String> lines = new ArrayList<>();
        lines.add("<b>Reporter Details:</b>");
        if (hasName) lines.add("• <b>Name:</b> " + escape(name.trim()));
        if (hasPhone) lines.add("• <b>Phone:</b> " + escape(phone.trim()));
        if (hasEmail) lines.add("• <b>Email:</b> " + escape(email.trim()));

        return String.join("\n", lines);
    }

    public static String cleanDescription(String description, int maxLength) {
        if (description == null || description.isBlank()) {
            return "<i>No description provided.</i>";
        }

        String text = escape(description.trim());
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength) + "...\n<i>(truncated)</i>";
        }
        return text;
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String getString(Map<String, Object> map, String key, String defaultVal) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultVal;
    }
}
