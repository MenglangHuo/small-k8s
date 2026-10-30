package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.DashboardReportResult;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketPriority;
import com.menglang.ticket.domain.model.TicketQuery;
import com.menglang.ticket.domain.model.TicketType;
import com.menglang.ticket.domain.port.in.TicketUseCase;
import com.menglang.ticket.presentation.request.CreateTicketRequest;
import com.menglang.ticket.presentation.request.UpdateTicketRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TicketListResponse;
import com.menglang.ticket.presentation.response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping({"/api/v1/tickets", "/api/ticket", "/api/tickets"})
@RequiredArgsConstructor
public class TicketController {

    private final TicketUseCase ticketUseCase;

    @GetMapping("/reports")
    public Mono<DashboardReportResult> reports(
            @RequestParam(required = false) Long company_id,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date,
            ServerWebExchange exchange) {

        Long companyId = company_id != null 
                ? company_id 
                : (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);

        Map<String, Object> filters = new HashMap<>();
        if (days != null) filters.put("days", days);
        if (from_date != null) filters.put("from_date", from_date);
        if (to_date != null) filters.put("to_date", to_date);

        return ticketUseCase.getDashboardReports(companyId, filters);
    }

    @GetMapping
    public Mono<ApiResponse<Map<String, Object>>> index(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20", name = "per_page") int perPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, name = "assignee_id") Long assigneeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "id", name = "sort_by") String sortBy,
            @RequestParam(defaultValue = "desc", name = "sort_dir") String sortDir,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        String finalSearch = (search != null && !search.isBlank()) ? search : q;

        TicketQuery query = TicketQuery.builder()
                .companyId(companyId)
                .page(page)
                .perPage(perPage)
                .status(status)
                .priority(priority)
                .type(type)
                .source(source)
                .assigneeId(assigneeId)
                .search(finalSearch)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        log.info("Fetching tickets: page={}, per_page={}, status={}, priority={}, search='{}'", page, perPage, status, priority, finalSearch);

        return ticketUseCase.listTickets(query)
                .map(facetResult -> {
                    List<TicketListResponse> content = facetResult.getContent().stream()
                            .map(TicketListResponse::fromDomain)
                            .toList();

                    Map<String, Object> pagination = Map.of(
                            "page", facetResult.getPage(),
                            "per_page", facetResult.getPerPage(),
                            "total", facetResult.getTotal(),
                            "total_pages", facetResult.getTotalPages(),
                            "has_next", facetResult.isHasNext(),
                            "has_previous", facetResult.isHasPrevious()
                    );

                    Map<String, Object> dataMap = Map.of(
                            "content", content,
                            "pagination", pagination,
                            "applied_filters", facetResult.getAppliedFilters(),
                            "facets", facetResult.getFacets()
                    );

                    return ApiResponse.success(dataMap, "Retrieved successfully.");
                });
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<TicketResponse>> store(
            @Valid @RequestBody CreateTicketRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        Ticket ticket = Ticket.builder()
                .companyId(companyId)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType() != null ? TicketType.fromValue(request.getType()) : TicketType.BUG)
                .priority(request.getPriority() != null ? TicketPriority.fromValue(request.getPriority()) : TicketPriority.MEDIUM)
                .source(request.getSource() != null ? request.getSource() : "portal")
                .externalRef(request.getExternalRef())
                .reporterName(request.getReporterName())
                .reporterEmail(request.getReporterEmail())
                .phone(request.getPhone() != null ? request.getPhone() : request.getReporterPhone())
                .dueAt(request.getDueAt())
                .metadata(request.getMetadata())
                .createdBy(userId)
                .build();

        log.info("Creating ticket: title='{}', type={}, priority={}, source={}", ticket.getTitle(), ticket.getType(), ticket.getPriority(), ticket.getSource());

        return ticketUseCase.createTicket(ticket, Flux.empty(), List.of())
                .map(created -> ApiResponse.success(TicketResponse.fromDomain(created), "Created successfully."));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<TicketResponse>> storeMultipart(ServerWebExchange exchange) {
        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);

        return exchange.getMultipartData().flatMap(multipartMap -> {
            String title = getFormField(multipartMap, "title");
            String description = getFormField(multipartMap, "description");
            if (description == null) {
                description = getFormField(multipartMap, "descritpion");
            }
            String type = getFormField(multipartMap, "type", "bug");
            String priority = getFormField(multipartMap, "priority", "medium");
            String source = getFormField(multipartMap, "source", "portal");
            String reporterName = getFormField(multipartMap, "reporter_name");
            String reporterEmail = getFormField(multipartMap, "reporter_email");
            String phone = getFormField(multipartMap, "reporter_phone", getFormField(multipartMap, "phone"));
            String externalRef = getFormField(multipartMap, "external_ref");

            Map<String, Object> metadata = new HashMap<>();
            if (phone != null) metadata.put("reporter_phone", phone);
            String clientName = getFormField(multipartMap, "client_name");
            if (clientName != null) metadata.put("api_client_name", clientName);

            Ticket ticket = Ticket.builder()
                    .companyId(companyId)
                    .title(title)
                    .description(description)
                    .type(TicketType.fromValue(type))
                    .priority(TicketPriority.fromValue(priority))
                    .source(source)
                    .externalRef(externalRef)
                    .reporterName(reporterName)
                    .reporterEmail(reporterEmail)
                    .phone(phone)
                    .metadata(metadata)
                    .createdBy(userId)
                    .build();

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

            log.info("Creating multipart ticket: title='{}', type={}, priority={}, filesCount={}", ticket.getTitle(), ticket.getType(), ticket.getPriority(), fileParts.size());

            return ticketUseCase.createTicket(ticket, Flux.fromIterable(fileParts), List.of())
                    .map(created -> ApiResponse.success(TicketResponse.fromDomain(created), "Created successfully."));
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

    @GetMapping("/{id}")
    public Mono<ApiResponse<TicketResponse>> show(
            @PathVariable String id,
            @RequestParam(defaultValue = "true", name = "include_internal") boolean includeInternal,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        log.info("Fetching ticket details for id: {}", id);

        return ticketUseCase.getTicketById(id, companyId, includeInternal)
                .map(ticket -> ApiResponse.success(TicketResponse.fromDomain(ticket)));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<TicketResponse>> update(
            @PathVariable String id,
            @RequestBody UpdateTicketRequest request,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);
        log.info("Updating ticket '{}': updates={}", id, request.toUpdateMap());

        return ticketUseCase.updateTicket(id, companyId, request.toUpdateMap(), userId)
                .map(updated -> ApiResponse.success(TicketResponse.fromDomain(updated), "Updated successfully."));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> destroy(
            @PathVariable String id,
            ServerWebExchange exchange) {

        Long companyId = (Long) exchange.getAttributes().getOrDefault("COMPANY_ID", 1L);
        Long userId = (Long) exchange.getAttributes().getOrDefault("USER_ID", 1L);
        log.info("Deleting ticket '{}' by user {}", id, userId);

        return ticketUseCase.deleteTicket(id, companyId, userId)
                .map(deleted -> ApiResponse.success(null, "Deleted successfully."));
    }
}
