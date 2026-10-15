package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.DashboardReportResult;
import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketFacetResult;
import com.menglang.ticket.domain.model.TicketQuery;
import com.menglang.ticket.domain.port.out.TicketRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TicketDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoTicketRepositoryAdapter implements TicketRepositoryPort {

    private final SpringDataTicketRepository ticketRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Ticket> save(Ticket ticket) {
        TicketDocument doc = TicketDocMapper.toDocument(ticket);
        return ticketRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Ticket> findById(String id, Long companyId) {
        return ticketRepository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Ticket> findByTicketKey(String ticketKey, Long companyId) {
        return ticketRepository.findByTicketKeyAndCompanyIdAndIsDeletedFalse(ticketKey, companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<TicketFacetResult> listWithFacets(TicketQuery query) {
        List<Criteria> baseCriteriaList = new ArrayList<>();
        baseCriteriaList.add(Criteria.where("company_id").is(query.getCompanyId()));
        baseCriteriaList.add(Criteria.where("is_deleted").is(false));

        // Multilingual fuzzy/substring search
        if (query.getSearch() != null && !query.getSearch().isBlank()) {
            String term = query.getSearch().trim();
            Pattern regex = Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE);
            baseCriteriaList.add(new Criteria().orOperator(
                    Criteria.where("ticket_key").regex(regex),
                    Criteria.where("title").regex(regex),
                    Criteria.where("description").regex(regex),
                    Criteria.where("reporter_name").regex(regex),
                    Criteria.where("phone").regex(regex),
                    Criteria.where("reporter_email").regex(regex),
                    Criteria.where("metadata.api_client_name").regex(regex)
            ));
        }

        Criteria baseCriteria = new Criteria().andOperator(baseCriteriaList.toArray(new Criteria[0]));

        // Secondary filters for paginated content
        List<Criteria> contentFilters = new ArrayList<>();
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            contentFilters.add(Criteria.where("status").is(query.getStatus().trim().toUpperCase()));
        }
        if (query.getPriority() != null && !query.getPriority().isBlank()) {
            contentFilters.add(Criteria.where("priority").is(query.getPriority().trim().toUpperCase()));
        }
        if (query.getType() != null && !query.getType().isBlank()) {
            contentFilters.add(Criteria.where("type").is(query.getType().trim().toUpperCase()));
        }
        if (query.getSource() != null && !query.getSource().isBlank()) {
            contentFilters.add(Criteria.where("source").is(query.getSource().trim()));
        }
        if (query.getAssigneeId() != null) {
            contentFilters.add(Criteria.where("assignee.id").is(query.getAssigneeId()));
        }

        List<AggregationOperation> contentPipeline = new ArrayList<>();
        if (!contentFilters.isEmpty()) {
            contentPipeline.add(match(new Criteria().andOperator(contentFilters.toArray(new Criteria[0]))));
        }

        // Sorting
        String sortField = resolveSortField(query.getSortBy());
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        contentPipeline.add(sort(Sort.by(direction, sortField)));

        // Pagination
        int page = query.getClampedPage();
        int perPage = query.getClampedPerPage();
        long skip = (long) (page - 1) * perPage;
        contentPipeline.add(skip(skip));
        contentPipeline.add(limit(perPage));

        // Facet Pipeline for Statuses
        List<AggregationOperation> statusFacetOps = List.of(
                group("status").count().as("count")
        );

        // Facet Pipeline for Priorities
        List<AggregationOperation> priorityFacetOps = List.of(
                group("priority").count().as("count")
        );

        // Filtered Count Pipeline
        List<AggregationOperation> totalFilteredOps = new ArrayList<>();
        if (!contentFilters.isEmpty()) {
            totalFilteredOps.add(match(new Criteria().andOperator(contentFilters.toArray(new Criteria[0]))));
        }
        totalFilteredOps.add(count().as("total"));

        FacetOperation facetOperation = facet()
                .and(contentPipeline.toArray(new AggregationOperation[0])).as("content")
                .and(totalFilteredOps.toArray(new AggregationOperation[0])).as("filteredTotal")
                .and(statusFacetOps.toArray(new AggregationOperation[0])).as("statusFacets")
                .and(priorityFacetOps.toArray(new AggregationOperation[0])).as("priorityFacets")
                .and(count().as("unfilteredTotal")).as("unfilteredTotal");

        Aggregation aggregation = newAggregation(match(baseCriteria), facetOperation);

        return mongoTemplate.aggregate(aggregation, "tickets", Document.class)
                .next()
                .map(rawDoc -> buildFacetResult(rawDoc, query, page, perPage));
    }

    private String resolveSortField(String sortBy) {
        if (sortBy == null) return "created_at";
        return switch (sortBy.toLowerCase()) {
            case "id" -> "_id";
            case "created_at" -> "created_at";
            case "updated_at" -> "updated_at";
            case "priority" -> "priority";
            case "status" -> "status";
            case "due_at" -> "due_at";
            case "ticket_key" -> "ticket_key";
            default -> "created_at";
        };
    }

    @SuppressWarnings("unchecked")
    private TicketFacetResult buildFacetResult(Document doc, TicketQuery query, int page, int perPage) {
        List<Document> contentDocs = doc.getList("content", Document.class, List.of());
        List<Ticket> tickets = contentDocs.stream()
                .map(d -> mongoTemplate.getConverter().read(TicketDocument.class, d))
                .map(TicketDocMapper::toDomain)
                .toList();

        List<Document> filteredTotalList = doc.getList("filteredTotal", Document.class, List.of());
        long total = 0;
        if (!filteredTotalList.isEmpty()) {
            Number totalNum = filteredTotalList.get(0).get("total", Number.class);
            if (totalNum != null) total = totalNum.longValue();
        }

        List<Document> statusList = doc.getList("statusFacets", Document.class, List.of());
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("open", 0);
        statusMap.put("in_progress", 0);
        statusMap.put("resolved", 0);
        statusMap.put("closed", 0);
        for (Document d : statusList) {
            String status = d.getString("_id");
            Number cnt = d.get("count", Number.class);
            if (status != null && cnt != null) {
                statusMap.put(status.toLowerCase(), cnt.intValue());
            }
        }

        List<Document> priorityList = doc.getList("priorityFacets", Document.class, List.of());
        Map<String, Integer> priorityMap = new HashMap<>();
        priorityMap.put("critical", 0);
        priorityMap.put("high", 0);
        priorityMap.put("medium", 0);
        priorityMap.put("low", 0);
        for (Document d : priorityList) {
            String priority = d.getString("_id");
            Number cnt = d.get("count", Number.class);
            if (priority != null && cnt != null) {
                priorityMap.put(priority.toLowerCase(), cnt.intValue());
            }
        }

        List<Document> unfilteredList = doc.getList("unfilteredTotal", Document.class, List.of());
        long unfilteredTotal = 0;
        if (!unfilteredList.isEmpty()) {
            Number cnt = unfilteredList.get(0).get("unfilteredTotal", Number.class);
            if (cnt != null) unfilteredTotal = cnt.longValue();
        }

        Map<String, Object> facets = new HashMap<>();
        facets.put("status", statusMap);
        facets.put("priority", priorityMap);
        facets.put("total", unfilteredTotal > 0 ? unfilteredTotal : total);

        Map<String, Object> appliedFilters = new HashMap<>();
        appliedFilters.put("status", query.getStatus());
        appliedFilters.put("priority", query.getPriority());
        appliedFilters.put("type", query.getType());
        appliedFilters.put("source", query.getSource());
        appliedFilters.put("assignee_id", query.getAssigneeId());
        appliedFilters.put("search", query.getSearch());
        appliedFilters.put("sort_by", query.getSortBy());
        appliedFilters.put("sort_dir", query.getSortDir());

        int totalPages = perPage > 0 ? (int) Math.ceil((double) total / perPage) : 1;

        return TicketFacetResult.builder()
                .content(tickets)
                .total(total)
                .page(page)
                .perPage(perPage)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .appliedFilters(appliedFilters)
                .facets(facets)
                .build();
    }

    @Override
    public Mono<DashboardReportResult> getDashboardMetrics(Long companyId, Map<String, Object> filters) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("company_id").is(companyId));
        criteriaList.add(Criteria.where("is_deleted").is(false));

        String dateFrom = (String) (filters.get("date_from") != null ? filters.get("date_from") : filters.get("from_date"));
        String dateTo = (String) (filters.get("date_to") != null ? filters.get("date_to") : filters.get("to_date"));

        if (dateFrom != null && !dateFrom.isBlank()) {
            criteriaList.add(Criteria.where("created_at").gte(Instant.parse(dateFrom.contains("T") ? dateFrom : dateFrom + "T00:00:00Z")));
        }
        if (dateTo != null && !dateTo.isBlank()) {
            criteriaList.add(Criteria.where("created_at").lte(Instant.parse(dateTo.contains("T") ? dateTo : dateTo + "T23:59:59Z")));
        }
        if ((dateFrom == null || dateFrom.isBlank()) && (dateTo == null || dateTo.isBlank()) && filters.get("days") != null) {
            int days = Integer.parseInt(filters.get("days").toString());
            criteriaList.add(Criteria.where("created_at").gte(Instant.now().minus(days, ChronoUnit.DAYS)));
        }

        Criteria matchCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        Aggregation agg = newAggregation(
                match(matchCriteria),
                facet()
                        .and(count().as("total")).as("total")
                        .and(group("status").count().as("count")).as("byStatus")
                        .and(group("priority").count().as("count")).as("byPriority")
                        .and(group("type").count().as("count")).as("byType")
        );

        return mongoTemplate.aggregate(agg, "tickets", Document.class)
                .next()
                .map(this::buildDashboardReport);
    }

    private DashboardReportResult buildDashboardReport(Document doc) {
        List<Document> totalList = doc.getList("total", Document.class, List.of());
        int total = 0;
        if (!totalList.isEmpty() && totalList.get(0).get("total") instanceof Number n) {
            total = n.intValue();
        }

        Map<String, Object> byStatus = new HashMap<>(Map.of("open", 0, "in_progress", 0, "resolved", 0, "closed", 0));
        for (Document d : doc.getList("byStatus", Document.class, List.of())) {
            String k = d.getString("_id");
            if (k != null && d.get("count") instanceof Number n) byStatus.put(k.toLowerCase(), n.intValue());
        }

        Map<String, Object> byPriority = new HashMap<>(Map.of("critical", 0, "high", 0, "medium", 0, "low", 0));
        for (Document d : doc.getList("byPriority", Document.class, List.of())) {
            String k = d.getString("_id");
            if (k != null && d.get("count") instanceof Number n) byPriority.put(k.toLowerCase(), n.intValue());
        }

        Map<String, Object> byType = new HashMap<>(Map.of("bug", 0, "enhancement", 0, "question", 0, "task", 0));
        for (Document d : doc.getList("byType", Document.class, List.of())) {
            String k = d.getString("_id");
            if (k != null && d.get("count") instanceof Number n) byType.put(k.toLowerCase(), n.intValue());
        }

        int resolved = (int) byStatus.getOrDefault("resolved", 0);
        int closed = (int) byStatus.getOrDefault("closed", 0);
        double resolutionRate = total > 0 ? Math.round(((double) (resolved + closed) / total * 100.0) * 10.0) / 10.0 : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_tickets", total);
        summary.put("open_tickets", byStatus.get("open"));
        summary.put("in_progress_tickets", byStatus.get("in_progress"));
        summary.put("resolved_tickets", byStatus.get("resolved"));
        summary.put("closed_tickets", byStatus.get("closed"));
        summary.put("critical_tickets", byPriority.get("critical"));
        summary.put("high_tickets", byPriority.get("high"));
        summary.put("medium_tickets", byPriority.get("medium"));
        summary.put("low_tickets", byPriority.get("low"));
        summary.put("resolution_rate", resolutionRate);
        summary.put("avg_resolution_hours", 0.0);

        return DashboardReportResult.builder()
                .summary(summary)
                .byStatus(byStatus)
                .byType(byType)
                .byPriority(byPriority)
                .bySource(new ArrayList<>())
                .topClients(new ArrayList<>())
                .trends(new ArrayList<>())
                .success(true)
                .build();
    }

    @Override
    public Mono<Boolean> softDelete(String id, Long companyId, Long deletedBy) {
        Query query = Query.query(Criteria.where("_id").is(id).and("company_id").is(companyId));
        Update update = new Update()
                .set("is_deleted", true)
                .set("updated_by", deletedBy)
                .set("updated_at", Instant.now());

        return mongoTemplate.updateFirst(query, update, TicketDocument.class)
                .map(res -> res.getModifiedCount() > 0);
    }
}
