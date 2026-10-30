package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketPriority;
import com.menglang.ticket.domain.model.TicketType;
import com.menglang.ticket.domain.port.in.TicketUseCase;
import com.menglang.ticket.presentation.request.ClientCreateTicketRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/client/tickets", "/api/ticket/client"})
@RequiredArgsConstructor
public class ClientTicketController {

    private final TicketUseCase ticketUseCase;

    /**
     * Submit ticket with JSON body (supports base64 attachments in files[]).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<TicketResponse>> storeJson(
            @Valid @RequestBody ClientCreateTicketRequest request,
            ServerWebExchange exchange) {

        ApiClient client = (ApiClient) exchange.getAttributes().get("API_CLIENT");
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        Map<String, Object> metadata = request.getMetadata() != null ? new HashMap<>(request.getMetadata()) : new HashMap<>();
        if (client != null) {
            metadata.put("api_client_id", client.getClientId());
            metadata.put("api_client_name", client.getName());
        }
        metadata.put("reporter_phone", request.getReporterPhone());

        Ticket ticket = Ticket.builder()
                .companyId(companyId)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(TicketType.fromValue(request.getType()))
                .priority(TicketPriority.fromValue(request.getPriority()))
                .source("api")
                .externalRef(request.getExternalRef())
                .reporterName(request.getReporterName())
                .reporterEmail(request.getReporterEmail())
                .phone(request.getReporterPhone())
                .metadata(metadata)
                .createdBy(0L)
                .build();

        log.info("Client API ticket submitted (JSON): title='{}', client='{}', type='{}', priority='{}'",
                request.getTitle(), client != null ? client.getName() : "anonymous", request.getType(), request.getPriority());

        return ticketUseCase.createTicket(ticket, Flux.empty(), request.getFiles())
                .map(created -> ApiResponse.success(TicketResponse.fromDomain(created), "Ticket has been submitted successfully."));
    }

    /**
     * Submit ticket with Multipart Form-Data (supports binary files[] and text fields).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<TicketResponse>> storeMultipart(ServerWebExchange exchange) {
        ApiClient client = (ApiClient) exchange.getAttributes().get("API_CLIENT");
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        return exchange.getMultipartData().flatMap(multipartMap -> {
            String title = getFormField(multipartMap, "title");
            String description = getFormField(multipartMap, "description");
            String type = getFormField(multipartMap, "type", "bug");
            String priority = getFormField(multipartMap, "priority", "medium");
            String reporterName = getFormField(multipartMap, "reporter_name");
            String reporterPhone = getFormField(multipartMap, "reporter_phone");
            String reporterEmail = getFormField(multipartMap, "reporter_email");
            String externalRef = getFormField(multipartMap, "external_ref");

            Map<String, Object> metadata = new HashMap<>();
            if (client != null) {
                metadata.put("api_client_id", client.getClientId());
                metadata.put("api_client_name", client.getName());
            }
            if (reporterPhone != null) {
                metadata.put("reporter_phone", reporterPhone);
            }

            Ticket ticket = Ticket.builder()
                    .companyId(companyId)
                    .title(title)
                    .description(description)
                    .type(TicketType.fromValue(type))
                    .priority(TicketPriority.fromValue(priority))
                    .source("api")
                    .externalRef(externalRef)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail)
                    .phone(reporterPhone)
                    .metadata(metadata)
                    .createdBy(0L)
                    .build();

            // Collect binary FileParts from keys: files, files[], file, attachments, attachment
            List<FilePart> fileParts = new ArrayList<>();
            for (String key : List.of("files", "files[]", "file", "attachments", "attachments[]", "attachment")) {
                List<Part> parts = multipartMap.get(key);
                if (parts != null) {
                    for (Part part : parts) {
                        if (part instanceof FilePart fp) {
                            fileParts.add(fp);
                        }
                    }
                }
            }

            log.info("Client API ticket submitted (multipart): title='{}', client='{}', type='{}', priority='{}', filesCount={}",
                    title, client != null ? client.getName() : "anonymous", type, priority, fileParts.size());

            return ticketUseCase.createTicket(ticket, Flux.fromIterable(fileParts), List.of())
                    .map(created -> ApiResponse.success(TicketResponse.fromDomain(created), "Ticket has been submitted successfully."));
        });
    }

    private String getFormField(Map<String, List<Part>> map, String key) {
        return getFormField(map, key, null);
    }

    private String getFormField(Map<String, List<Part>> map, String key, String defaultVal) {
        List<Part> parts = map.get(key);
        if (parts != null && !parts.isEmpty() && parts.get(0) instanceof FormFieldPart ffp) {
            return ffp.value();
        }
        return defaultVal;
    }
}
