package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.model.NotificationOutbox;
import com.menglang.ticket.domain.model.TelegramInvite;
import com.menglang.ticket.domain.model.TelegramSubscriber;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketAttachment;
import com.menglang.ticket.domain.model.TicketComment;
import com.menglang.ticket.domain.model.TicketEvent;
import com.menglang.ticket.infrastructure.persistence.document.ApiClientDocument;
import com.menglang.ticket.infrastructure.persistence.document.NotificationOutboxDocument;
import com.menglang.ticket.infrastructure.persistence.document.TelegramInviteDocument;
import com.menglang.ticket.infrastructure.persistence.document.TelegramSubscriberDocument;
import com.menglang.ticket.infrastructure.persistence.document.TicketAttachmentDocument;
import com.menglang.ticket.infrastructure.persistence.document.TicketCommentDocument;
import com.menglang.ticket.infrastructure.persistence.document.TicketDocument;
import com.menglang.ticket.infrastructure.persistence.document.TicketEventDocument;

import java.util.ArrayList;

public class TicketDocMapper {

    public static Ticket toDomain(TicketDocument doc) {
        if (doc == null) return null;
        return Ticket.builder()
                .id(doc.getId())
                .publicId(doc.getPublicId())
                .companyId(doc.getCompanyId())
                .number(doc.getNumber())
                .ticketKey(doc.getTicketKey())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .type(doc.getType())
                .status(doc.getStatus())
                .priority(doc.getPriority())
                .source(doc.getSource())
                .externalRef(doc.getExternalRef())
                .reporterName(doc.getReporterName())
                .reporterEmail(doc.getReporterEmail())
                .phone(doc.getPhone())
                .metadata(doc.getMetadata() != null ? doc.getMetadata() : new java.util.HashMap<>())
                .assignee(doc.getAssignee())
                .reporter(doc.getReporter())
                .company(doc.getCompany())
                .dueAt(doc.getDueAt())
                .firstResponseAt(doc.getFirstResponseAt())
                .resolvedAt(doc.getResolvedAt())
                .closedAt(doc.getClosedAt())
                .isDeleted(doc.getIsDeleted())
                .createdBy(doc.getCreatedBy())
                .updatedBy(doc.getUpdatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .attachments(new ArrayList<>())
                .comments(new ArrayList<>())
                .build();
    }

    public static TicketDocument toDocument(Ticket domain) {
        if (domain == null) return null;
        return TicketDocument.builder()
                .id(domain.getId())
                .publicId(domain.getPublicId())
                .companyId(domain.getCompanyId())
                .number(domain.getNumber())
                .ticketKey(domain.getTicketKey())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .type(domain.getType())
                .status(domain.getStatus())
                .priority(domain.getPriority())
                .source(domain.getSource())
                .externalRef(domain.getExternalRef())
                .reporterName(domain.getReporterName())
                .reporterEmail(domain.getReporterEmail())
                .phone(domain.getPhone())
                .metadata(domain.getMetadata())
                .assignee(domain.getAssignee())
                .reporter(domain.getReporter())
                .company(domain.getCompany())
                .dueAt(domain.getDueAt())
                .firstResponseAt(domain.getFirstResponseAt())
                .resolvedAt(domain.getResolvedAt())
                .closedAt(domain.getClosedAt())
                .isDeleted(domain.getIsDeleted())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static TicketComment toDomain(TicketCommentDocument doc) {
        if (doc == null) return null;
        return TicketComment.builder()
                .id(doc.getId())
                .ticketId(doc.getTicketId())
                .userId(doc.getUserId())
                .body(doc.getBody())
                .isInternal(doc.getIsInternal())
                .author(doc.getAuthor())
                .metadata(doc.getMetadata())
                .isDeleted(doc.getIsDeleted())
                .createdBy(doc.getCreatedBy())
                .updatedBy(doc.getUpdatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .attachments(new ArrayList<>())
                .build();
    }

    public static TicketCommentDocument toDocument(TicketComment domain) {
        if (domain == null) return null;
        return TicketCommentDocument.builder()
                .id(domain.getId())
                .ticketId(domain.getTicketId())
                .userId(domain.getUserId())
                .body(domain.getBody())
                .isInternal(domain.getIsInternal())
                .author(domain.getAuthor())
                .metadata(domain.getMetadata())
                .isDeleted(domain.getIsDeleted())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static TicketAttachment toDomain(TicketAttachmentDocument doc) {
        if (doc == null) return null;
        return TicketAttachment.builder()
                .id(doc.getId())
                .ticketId(doc.getTicketId())
                .commentId(doc.getCommentId())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedBy(doc.getUploadedBy())
                .isDeleted(doc.getIsDeleted())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static TicketAttachmentDocument toDocument(TicketAttachment domain) {
        if (domain == null) return null;
        return TicketAttachmentDocument.builder()
                .id(domain.getId())
                .ticketId(domain.getTicketId())
                .commentId(domain.getCommentId())
                .fileName(domain.getFileName())
                .filePath(domain.getFilePath())
                .fileSize(domain.getFileSize())
                .mimeType(domain.getMimeType())
                .uploadedBy(domain.getUploadedBy())
                .isDeleted(domain.getIsDeleted())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static NotificationOutbox toDomain(NotificationOutboxDocument doc) {
        if (doc == null) return null;
        return NotificationOutbox.builder()
                .id(doc.getId())
                .ticketId(doc.getTicketId())
                .companyId(doc.getCompanyId())
                .channel(doc.getChannel())
                .eventType(doc.getEventType())
                .payload(doc.getPayload())
                .status(doc.getStatus())
                .attempts(doc.getAttempts())
                .lastError(doc.getLastError())
                .availableAt(doc.getAvailableAt())
                .sentAt(doc.getSentAt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static NotificationOutboxDocument toDocument(NotificationOutbox domain) {
        if (domain == null) return null;
        return NotificationOutboxDocument.builder()
                .id(domain.getId())
                .ticketId(domain.getTicketId())
                .companyId(domain.getCompanyId())
                .channel(domain.getChannel())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .status(domain.getStatus())
                .attempts(domain.getAttempts())
                .lastError(domain.getLastError())
                .availableAt(domain.getAvailableAt())
                .sentAt(domain.getSentAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static ApiClient toDomain(ApiClientDocument doc) {
        if (doc == null) return null;
        return ApiClient.builder()
                .id(doc.getId())
                .companyId(doc.getCompanyId())
                .clientId(doc.getClientId())
                .clientSecret(doc.getClientSecret())
                .name(doc.getName())
                .isActive(doc.getIsActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static ApiClientDocument toDocument(ApiClient domain) {
        if (domain == null) return null;
        return ApiClientDocument.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .clientId(domain.getClientId())
                .clientSecret(domain.getClientSecret())
                .name(domain.getName())
                .isActive(domain.getIsActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static TelegramSubscriber toDomain(TelegramSubscriberDocument doc) {
        if (doc == null) return null;
        return TelegramSubscriber.builder()
                .id(doc.getId())
                .companyId(doc.getCompanyId())
                .telegramChatId(doc.getTelegramChatId())
                .telegramUsername(doc.getTelegramUsername())
                .inviteToken(doc.getInviteToken())
                .isActive(doc.getIsActive())
                .subscribedAt(doc.getSubscribedAt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static TelegramSubscriberDocument toDocument(TelegramSubscriber domain) {
        if (domain == null) return null;
        return TelegramSubscriberDocument.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .telegramChatId(domain.getTelegramChatId())
                .telegramUsername(domain.getTelegramUsername())
                .inviteToken(domain.getInviteToken())
                .isActive(domain.getIsActive())
                .subscribedAt(domain.getSubscribedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static TelegramInvite toDomain(TelegramInviteDocument doc) {
        if (doc == null) return null;
        return TelegramInvite.builder()
                .id(doc.getId())
                .companyId(doc.getCompanyId())
                .token(doc.getToken())
                .isUsed(doc.getIsUsed())
                .expiresAt(doc.getExpiresAt())
                .createdBy(doc.getCreatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public static TelegramInviteDocument toDocument(TelegramInvite domain) {
        if (domain == null) return null;
        return TelegramInviteDocument.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .token(domain.getToken())
                .isUsed(domain.getIsUsed())
                .expiresAt(domain.getExpiresAt())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static TicketEventDocument toDocument(TicketEvent domain) {
        if (domain == null) return null;
        return TicketEventDocument.builder()
                .id(domain.getId())
                .ticketId(domain.getTicketId())
                .eventType(domain.getEventType())
                .actorUserId(domain.getActorUserId())
                .actorType(domain.getActorType())
                .payload(domain.getPayload())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
