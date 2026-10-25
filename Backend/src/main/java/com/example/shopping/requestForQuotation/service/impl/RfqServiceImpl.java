package com.example.shopping.requestForQuotation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.requestForQuotation.dto.request.CreateRfqRequest;
import com.example.shopping.requestForQuotation.dto.request.ItemWinnerRequest;
import com.example.shopping.requestForQuotation.dto.request.RequestRfqRequoteRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqActionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqDecisionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqItemRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqScoreCriterionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqSearchRequest;
import com.example.shopping.requestForQuotation.dto.request.ScoreRequest;
import com.example.shopping.requestForQuotation.dto.request.SelectRfqWinnersRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqQuoteScoresRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqRequest;
import com.example.shopping.requestForQuotation.dto.response.InvitedSupplierResponse;
import com.example.shopping.requestForQuotation.dto.response.ItemAwardResponse;
import com.example.shopping.requestForQuotation.dto.response.ItemResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqAuditLogResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqSummaryResponse;
import com.example.shopping.requestForQuotation.dto.response.ScoreCriterionResponse;
import com.example.shopping.requestForQuotation.entity.Rfq;
import com.example.shopping.requestForQuotation.entity.RfqAuditLog;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import com.example.shopping.requestForQuotation.entity.RfqItem;
import com.example.shopping.requestForQuotation.entity.RfqItemAward;
import com.example.shopping.requestForQuotation.entity.RfqQuoteItem;
import com.example.shopping.requestForQuotation.entity.RfqQuoteRound;
import com.example.shopping.requestForQuotation.entity.RfqQuoteScore;
import com.example.shopping.requestForQuotation.entity.RfqScoreCriterion;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import com.example.shopping.requestForQuotation.enums.RfqQuoteStatus;
import com.example.shopping.requestForQuotation.enums.RfqReviewDecision;
import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;
import com.example.shopping.requestForQuotation.enums.RfqStatus;
import com.example.shopping.requestForQuotation.kafka.event.RfqInitialApprovalEvent;
import com.example.shopping.requestForQuotation.mapper.SupplierInvitationMapper;
import com.example.shopping.requestForQuotation.repository.RfqAuditLogRepository;
import com.example.shopping.requestForQuotation.repository.RfqInvitationTokenRepository;
import com.example.shopping.requestForQuotation.repository.RfqInvitedSupplierRepository;
import com.example.shopping.requestForQuotation.repository.RfqQuoteRoundRepository;
import com.example.shopping.requestForQuotation.repository.RfqRepository;
import com.example.shopping.requestForQuotation.service.RfqService;
import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.enums.SupplierStatus;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.user.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RfqServiceImpl implements RfqService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final Map<RfqScoringCriterion, BigDecimal> DEFAULT_WEIGHTS = Map.of(
            RfqScoringCriterion.PRICE, new BigDecimal("40.00"),
            RfqScoringCriterion.TECHNICAL, new BigDecimal("25.00"),
            RfqScoringCriterion.DELIVERY, new BigDecimal("15.00"),
            RfqScoringCriterion.PAYMENT, new BigDecimal("10.00"),
            RfqScoringCriterion.REPUTATION, new BigDecimal("10.00"));

    RfqRepository rfqRepository;
    RfqInvitedSupplierRepository invitedSupplierRepository;
    RfqQuoteRoundRepository quoteRoundRepository;
    RfqInvitationTokenRepository invitationTokenRepository;
    RfqAuditLogRepository auditLogRepository;
    ProcurementTicketRepository ticketRepository;
    SupplierRepository supplierRepository;
    UserRepository userRepository;
    SupplierInvitationMapper quoteMapper;
    ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RfqDetailResponse create(CreateRfqRequest request, String username, boolean isAdmin) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(request.procurementTicketId())
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());
        if (ticket.getStatus() != ProcurementStatus.APPROVED) {
            throw ErrorEnum.RFQ_TICKET_NOT_APPROVED.ex();
        }
        if (!isAdmin && !Objects.equals(ticket.getMakerUsername(), username)) {
            throw ErrorEnum.RFQ_TICKET_ACCESS_DENIED.ex();
        }

        LocalDateTime now = LocalDateTime.now();
        Rfq rfq = Rfq.builder()
                .rfqCode(generateRfqCode())
                .procurementTicket(ticket)
                .title(request.title().trim())
                .department(clean(request.department()))
                .deadline(request.deadline())
                .quoteValidityDays(request.quoteValidityDays())
                .deliveryLocation(clean(request.deliveryLocation()))
                .deliveryWindow(clean(request.deliveryWindow()))
                .currency(ticket.getCurrency() == null ? "VND" : ticket.getCurrency())
                .estimatedValue(BigDecimal.ZERO)
                .createdBy(username)
                .createdAt(now)
                .updatedAt(now)
                .exceptionReason(clean(request.exceptionReason()))
                .status(request.submitForApproval()
                        ? RfqStatus.PENDING_RFQ_APPROVAL : RfqStatus.DRAFT)
                .build();

        List<RfqItem> items = buildItems(rfq, request.items(), ticket, now);
        rfq.setItems(items);
        rfq.setEstimatedValue(estimate(items));
        rfq.setScoreCriteria(buildCriteria(rfq, request.scoreCriteria()));
        rfq.setInvitedSuppliers(buildInvites(rfq, request.supplierIds(), now));
        if (request.submitForApproval()) rfq.setSubmittedAt(now);
        Rfq saved = rfqRepository.save(rfq);
        addAudit(saved, username, "CREATED", Map.of("status", saved.getStatus().name()));
        return toDetail(saved);
    }

    @Override
    @Transactional
    public RfqDetailResponse update(UUID rfqId, UpdateRfqRequest request, String username, boolean isAdmin) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, isAdmin);
        if (rfq.getStatus() != RfqStatus.DRAFT && rfq.getStatus() != RfqStatus.RETURNED) {
            throw ErrorEnum.RFQ_CANNOT_EDIT.ex();
        }

        LocalDateTime now = LocalDateTime.now();
        rfq.setTitle(request.title().trim());
        rfq.setDepartment(clean(request.department()));
        rfq.setDeadline(request.deadline());
        rfq.setQuoteValidityDays(request.quoteValidityDays());
        rfq.setDeliveryLocation(clean(request.deliveryLocation()));
        rfq.setDeliveryWindow(clean(request.deliveryWindow()));
        rfq.setExceptionReason(clean(request.exceptionReason()));
        rfq.setUpdatedAt(now);

        for (RfqItem old : rfq.getItems()) old.setActive(false);
        List<RfqItem> replacementItems = buildItems(rfq, request.items(), rfq.getProcurementTicket(), now);
        rfq.getItems().addAll(replacementItems);
        rfq.setEstimatedValue(estimate(replacementItems));
        syncInvites(rfq, request.supplierIds(), now);
        rfq.getScoreCriteria().clear();
        rfq.getScoreCriteria().addAll(buildCriteria(rfq, request.scoreCriteria()));
        rfq.setReviewedBy(null);
        rfq.setReviewedAt(null);
        rfq.setReviewReason(null);
        addAudit(rfq, username, "UPDATED", Map.of("status", rfq.getStatus().name()));
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RfqSummaryResponse> search(RfqSearchRequest request) {
        String keyword = trimToNull(request.keyword());
        String department = trimToNull(request.department());
        String createdBy = trimToNull(request.createdBy());
        Specification<Rfq> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.status() != null)
                predicates.add(cb.equal(root.get("status"), request.status()));
            if (department != null)
                predicates.add(cb.like(cb.lower(root.get("department")),
                    "%" + department.toLowerCase(Locale.ROOT) + "%"));
            if (createdBy != null)
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            if (request.createdFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("createdAt"), request.createdFrom().atStartOfDay()));
            if (request.createdTo() != null)
                predicates.add(cb.lessThan(
                    root.get("createdAt"), request.createdTo().plusDays(1).atStartOfDay()));
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                var ticket = root.join("procurementTicket", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("rfqCode")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(ticket.get("ticketCode")), pattern),
                        cb.like(cb.lower(ticket.get("title")), pattern)));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        PageRequest page = PageRequest.of(request.page(), request.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return rfqRepository.findAll(spec, page).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RfqDetailResponse getById(UUID rfqId, String username, boolean canViewAll) {
        Rfq rfq = getEntity(rfqId);
        if (!canViewAll && !Objects.equals(rfq.getCreatedBy(), username)) {
            throw ErrorEnum.ACCESS_DENIED.ex();
        }
        return toDetail(rfq);
    }

    @Override
    @Transactional
    public RfqDetailResponse submit(UUID rfqId, String username, boolean isAdmin) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, isAdmin);
        if (rfq.getStatus() != RfqStatus.DRAFT && rfq.getStatus() != RfqStatus.RETURNED) {
            throw ErrorEnum.RFQ_CANNOT_SUBMIT.ex();
        }
        rfq.setStatus(RfqStatus.PENDING_RFQ_APPROVAL);
        rfq.setSubmittedAt(LocalDateTime.now());
        rfq.setUpdatedAt(LocalDateTime.now());
        addAudit(rfq, username, "SUBMITTED_FOR_APPROVAL", Map.of());
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse decide(UUID rfqId, RfqDecisionRequest request, String checkerUsername) {
        Rfq rfq = getEntity(rfqId);
        boolean initialReview = rfq.getStatus() == RfqStatus.PENDING_RFQ_APPROVAL;
        boolean resultReview = rfq.getStatus() == RfqStatus.PENDING_RESULT_APPROVAL;
        if (!initialReview && !resultReview) throw ErrorEnum.RFQ_CANNOT_REVIEW.ex();
        if (Objects.equals(rfq.getCreatedBy(), checkerUsername)) throw ErrorEnum.RFQ_SELF_REVIEW.ex();

        LocalDateTime now = LocalDateTime.now();
        rfq.setReviewedBy(checkerUsername);
        rfq.setReviewedAt(now);
        rfq.setReviewReason(request.reason().trim());
        switch (request.decision()) {
            case APPROVE -> rfq.setStatus(initialReview ? RfqStatus.SENT : RfqStatus.SELECTED_SUPPLIER);
            case RETURN -> rfq.setStatus(initialReview ? RfqStatus.RETURNED : RfqStatus.COMPARING);
            case REJECT -> rfq.setStatus(RfqStatus.REJECTED);
        }
        rfq.setUpdatedAt(now);
        addAudit(rfq, checkerUsername, "REVIEW_" + request.decision().name(),
                Map.of("reason", request.reason().trim(), "status", rfq.getStatus().name()));
        Rfq saved = rfqRepository.save(rfq);
        if (initialReview && request.decision() == RfqReviewDecision.APPROVE) {
            eventPublisher.publishEvent(new RfqInitialApprovalEvent(saved.getId(), checkerUsername));
        }
        return toDetail(saved);
    }

    @Override
    @Transactional
    public RfqDetailResponse cancel(UUID rfqId, RfqActionRequest request, String username, boolean isAdmin) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, isAdmin);
        if (rfq.getStatus() == RfqStatus.PO_CREATED || rfq.getStatus() == RfqStatus.SELECTED_SUPPLIER
                || rfq.getStatus() == RfqStatus.CANCELED || rfq.getStatus() == RfqStatus.REJECTED) {
            throw ErrorEnum.RFQ_CANNOT_CANCEL.ex();
        }
        rfq.setStatus(RfqStatus.CANCELED);
        rfq.setUpdatedAt(LocalDateTime.now());
        revokeTokens(rfq);
        addAudit(rfq, username, "CANCELED", Map.of("reason", request.reason().trim()));
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse closeQuotes(UUID rfqId, RfqActionRequest request,
                                         String username, boolean isAdmin) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, isAdmin);
        if (rfq.getStatus() != RfqStatus.SENT && rfq.getStatus() != RfqStatus.RECEIVING_QUOTES) {
            throw ErrorEnum.RFQ_CANNOT_CLOSE_QUOTES.ex();
        }
        LocalDateTime now = LocalDateTime.now();
        rfq.setStatus(RfqStatus.CLOSED_QUOTES);
        rfq.setUpdatedAt(now);
        revokeTokens(rfq);
        addAudit(rfq, username, "QUOTES_CLOSED", Map.of("reason", request.reason().trim()));
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse startComparison(UUID rfqId, String username, boolean isAdmin) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, isAdmin);
        if (rfq.getStatus() != RfqStatus.CLOSED_QUOTES) {
            throw ErrorEnum.RFQ_CANNOT_START_COMPARISON.ex();
        }
        rfq.setStatus(RfqStatus.COMPARING);
        rfq.setUpdatedAt(LocalDateTime.now());
        addAudit(rfq, username, "COMPARISON_STARTED", Map.of());
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse requestRequote(UUID rfqId, RequestRfqRequoteRequest request, String username) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, false);
        if (rfq.getStatus() != RfqStatus.CLOSED_QUOTES && rfq.getStatus() != RfqStatus.COMPARING) {
            throw ErrorEnum.RFQ_NOT_OPEN.ex();
        }
        Set<UUID> selectedIds = new HashSet<>(request.supplierIds());
        if (selectedIds.size() != request.supplierIds().size()) throw ErrorEnum.RFQ_SUPPLIER_SELECTION_INVALID.ex();
        for (UUID supplierId : selectedIds) {
            RfqInvitedSupplier invite = invitedSupplierRepository.findByRfq_IdAndSupplier_Id(rfqId, supplierId)
                    .orElseThrow(() -> ErrorEnum.RFQ_INVITATION_NOT_FOUND.ex());
            if (invite.getSupplier().getStatus() != SupplierStatus.ACTIVE) {
                throw ErrorEnum.RFQ_SUPPLIER_NOT_ACTIVE.ex();
            }
            invite.setStatus(RfqInviteStatus.INVITED);
            invite.setRespondedAt(null);
            invite.setInvitedAt(LocalDateTime.now());
            invitationTokenRepository.revokeActiveTokens(invite.getId());
        }
        rfq.setStatus(RfqStatus.RECEIVING_QUOTES);
        rfq.setUpdatedAt(LocalDateTime.now());
        addAudit(rfq, username, "REQUote_REQUESTED", Map.of(
                "supplierIds", selectedIds.stream().map(UUID::toString).toList(),
                "reason", request.reason().trim()));
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse updateQuoteScores(UUID rfqId, UUID quoteRoundId,
                                               UpdateRfqQuoteScoresRequest request, String username) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, false);
        if (rfq.getStatus() != RfqStatus.COMPARING) throw ErrorEnum.RFQ_QUOTE_CANNOT_SCORE.ex();
        RfqQuoteRound round = quoteRoundRepository.findById(quoteRoundId)
                .filter(candidate -> candidate.getRfq().getId().equals(rfqId))
                .orElseThrow(() -> ErrorEnum.RFQ_QUOTE_NOT_FOUND.ex());
        Set<RfqScoringCriterion> seen = new HashSet<>();
        Map<RfqScoringCriterion, RfqQuoteScore> existing = round.getScores().stream()
                .collect(Collectors.toMap(RfqQuoteScore::getCriterion, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        for (ScoreRequest scoreRequest : request.scores()) {
            if (!seen.add(scoreRequest.criterion()) || rfq.getScoreCriteria().stream()
                    .noneMatch(c -> c.getCriterion() == scoreRequest.criterion())) {
                throw ErrorEnum.RFQ_WEIGHT_INVALID.ex();
            }
            RfqQuoteScore score = existing.get(scoreRequest.criterion());
            if (score == null) {
                score = RfqQuoteScore.builder().quoteRound(round)
                        .criterion(scoreRequest.criterion()).build();
                round.getScores().add(score);
            }
            score.setScore(scoreRequest.score());
            score.setNote(clean(scoreRequest.note()));
            score.setUpdatedBy(username);
            score.setUpdatedAt(now);
        }
        rfq.setUpdatedAt(now);
        addAudit(rfq, username, "QUOTE_SCORES_UPDATED", Map.of(
                "quoteRoundId", quoteRoundId.toString(), "criteriaCount", seen.size()));
        return toDetail(rfqRepository.save(rfq));
    }

    @Override
    @Transactional
    public RfqDetailResponse selectWinners(UUID rfqId, SelectRfqWinnersRequest request, String username) {
        Rfq rfq = getEntity(rfqId);
        requireOwner(rfq, username, false);
        if (rfq.getStatus() != RfqStatus.COMPARING && rfq.getStatus() != RfqStatus.CLOSED_QUOTES) {
            throw ErrorEnum.RFQ_CANNOT_SELECT_WINNER.ex();
        }
        List<RfqItem> activeItems = rfq.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive())).toList();
        Map<UUID, RfqItemAward> awardByItem = rfq.getItemAwards().stream()
                .collect(Collectors.toMap(a -> a.getRfqItem().getId(), Function.identity(), (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();

        if (request.supplierId() != null) {
            SupplierEntity supplier = requireSupplierWithQuote(rfq, request.supplierId());
            String reason = clean(request.reason());
            for (RfqItem item : activeItems) {
                RfqQuoteItem quoteItem = requireSupplierQuote(rfq, supplier.getId(), item.getId());
                validateWinnerReason(rfq, item, quoteItem, supplier.getId(), reason);
                upsertAward(rfq, item, supplier, reason, username, now, awardByItem);
            }
            rfq.setSelectedSupplier(supplier);
            rfq.setSelectionReason(reason);
        }
        if (request.itemWinners() != null) {
            Set<UUID> seen = new HashSet<>();
            for (ItemWinnerRequest winner : request.itemWinners()) {
                if (!seen.add(winner.rfqItemId())) throw ErrorEnum.RFQ_WINNER_INVALID.ex();
                RfqItem item = activeItems.stream().filter(i -> i.getId().equals(winner.rfqItemId()))
                        .findFirst().orElseThrow(() -> ErrorEnum.RFQ_WINNER_INVALID.ex());
                SupplierEntity supplier = requireSupplierWithQuote(rfq, winner.supplierId());
                RfqQuoteItem quoteItem = requireSupplierQuote(rfq, supplier.getId(), item.getId());
                String reason = clean(winner.reason());
                validateWinnerReason(rfq, item, quoteItem, supplier.getId(), reason);
                upsertAward(rfq, item, supplier, reason, username, now, awardByItem);
            }
        }
        boolean allAwarded = activeItems.stream().allMatch(item -> awardByItem.containsKey(item.getId()));
        if (request.submitForApproval()) {
            if (!allAwarded) throw ErrorEnum.RFQ_AWARD_INCOMPLETE.ex();
            rfq.setStatus(RfqStatus.PENDING_RESULT_APPROVAL);
            rfq.setSubmittedAt(now);
        }
        rfq.setUpdatedAt(now);
        addAudit(rfq, username, request.submitForApproval() ? "WINNERS_SUBMITTED_FOR_APPROVAL" : "WINNERS_SELECTED",
                Map.of("awardCount", awardByItem.size()));
        return toDetail(rfqRepository.save(rfq));
    }

    private List<RfqItem> buildItems(Rfq rfq, List<RfqItemRequest> requests,
                                     ProcurementTicket ticket, LocalDateTime now) {
        Map<UUID, ProcurementItem> ticketItems = ticket.getItems().stream()
                .collect(Collectors.toMap(ProcurementItem::getId, Function.identity()));
        Set<UUID> seenProcurementItems = new HashSet<>();
        List<RfqItem> items = new ArrayList<>();
        for (RfqItemRequest request : requests) {
            ProcurementItem source = null;
            if (request.procurementItemId() != null) {
                source = ticketItems.get(request.procurementItemId());
                if (source == null || !seenProcurementItems.add(request.procurementItemId())) {
                    throw ErrorEnum.RFQ_ITEM_NOT_IN_TICKET.ex();
                }
            }
            String name = source == null ? request.itemName().trim() : source.getItemName();
            String sku = source == null ? clean(request.sku()) : source.getItemCode();
            String category = source == null ? clean(request.category()) : source.getCategory();
            int quantity = source == null ? request.quantity() : source.getQuantity();
            String unit = source == null ? clean(request.unit()) : source.getUnit();
            BigDecimal budget = source == null ? request.budgetUnitPrice() : source.getUnitPrice();
            items.add(RfqItem.builder().rfq(rfq).procurementItem(source).sku(sku)
                    .itemName(name).category(category).quantity(quantity).unit(unit)
                    .budgetUnitPrice(budget == null ? BigDecimal.ZERO : budget)
                    .active(true).createdAt(now).build());
        }
        return items;
    }

    private List<RfqScoreCriterion> buildCriteria(Rfq rfq, List<RfqScoreCriterionRequest> requests) {
        List<RfqScoreCriterionRequest> effective = requests;
        if (effective == null || effective.isEmpty()) {
            effective = DEFAULT_WEIGHTS.entrySet().stream()
                    .map(entry -> new RfqScoreCriterionRequest(entry.getKey(), entry.getValue())).toList();
        }
        Set<RfqScoringCriterion> distinct = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        List<RfqScoreCriterion> criteria = new ArrayList<>();
        for (RfqScoreCriterionRequest request : effective) {
            if (!distinct.add(request.criterion())) throw ErrorEnum.RFQ_WEIGHT_INVALID.ex();
            total = total.add(request.weightPercent());
            criteria.add(RfqScoreCriterion.builder().rfq(rfq).criterion(request.criterion())
                    .weightPercent(request.weightPercent()).build());
        }
        if (total.compareTo(ONE_HUNDRED) != 0) throw ErrorEnum.RFQ_WEIGHT_INVALID.ex();
        return criteria;
    }

    private List<RfqInvitedSupplier> buildInvites(Rfq rfq, List<UUID> supplierIds, LocalDateTime now) {
        Set<UUID> uniqueIds = new LinkedHashSet<>(supplierIds);
        if (uniqueIds.size() != supplierIds.size()) throw ErrorEnum.RFQ_SUPPLIER_SELECTION_INVALID.ex();
        if (uniqueIds.size() < 3 && (rfq.getExceptionReason() == null || rfq.getExceptionReason().isBlank())) {
            throw ErrorEnum.RFQ_SUPPLIERS_MINIMUM.ex();
        }
        List<SupplierEntity> suppliers = supplierRepository.findAllById(uniqueIds);
        if (suppliers.size() != uniqueIds.size()
                || suppliers.stream().anyMatch(s -> s.getStatus() != SupplierStatus.ACTIVE)) {
            throw ErrorEnum.RFQ_SUPPLIER_SELECTION_INVALID.ex();
        }
        return suppliers.stream().map(s -> RfqInvitedSupplier.builder().rfq(rfq).supplier(s)
                .status(RfqInviteStatus.INVITED).invitedAt(now).build()).toList();
    }

    private void syncInvites(Rfq rfq, List<UUID> supplierIds, LocalDateTime now) {
        Set<UUID> wanted = new LinkedHashSet<>(supplierIds);
        if (wanted.size() != supplierIds.size()) throw ErrorEnum.RFQ_SUPPLIER_SELECTION_INVALID.ex();
        if (wanted.size() < 3 && (rfq.getExceptionReason() == null || rfq.getExceptionReason().isBlank())) {
            throw ErrorEnum.RFQ_SUPPLIERS_MINIMUM.ex();
        }
        Map<UUID, RfqInvitedSupplier> existing = rfq.getInvitedSuppliers().stream()
                .collect(Collectors.toMap(i -> i.getSupplier().getId(), Function.identity()));
        List<SupplierEntity> suppliers = supplierRepository.findAllById(wanted);
        if (suppliers.size() != wanted.size()
                || suppliers.stream().anyMatch(s -> s.getStatus() != SupplierStatus.ACTIVE)) {
            throw ErrorEnum.RFQ_SUPPLIER_SELECTION_INVALID.ex();
        }
        Map<UUID, SupplierEntity> byId = suppliers.stream().collect(Collectors.toMap(SupplierEntity::getId, Function.identity()));
        for (RfqInvitedSupplier old : rfq.getInvitedSuppliers()) {
            UUID id = old.getSupplier().getId();
            if (!wanted.contains(id)) {
                old.setStatus(RfqInviteStatus.REVOKED);
                invitationTokenRepository.revokeActiveTokens(old.getId());
            }
        }
        for (UUID supplierId : wanted) {
            RfqInvitedSupplier invite = existing.get(supplierId);
            if (invite == null) {
                rfq.getInvitedSuppliers().add(RfqInvitedSupplier.builder().rfq(rfq)
                        .supplier(byId.get(supplierId)).status(RfqInviteStatus.INVITED).invitedAt(now).build());
            } else if (invite.getStatus() == RfqInviteStatus.REVOKED) {
                invite.setStatus(RfqInviteStatus.INVITED);
                invite.setInvitedAt(now);
                invite.setRespondedAt(null);
            }
        }
    }

    private void validateWinnerReason(Rfq rfq, RfqItem item, RfqQuoteItem chosen,
                                      UUID chosenSupplierId, String reason) {
        BigDecimal lowest = rfq.getQuoteRounds().stream()
                .filter(round -> round.getStatus() == RfqQuoteStatus.SUBMITTED)
                .flatMap(round -> round.getItems().stream())
                .filter(line -> line.getRfqItem().getId().equals(item.getId())
                        && Boolean.TRUE.equals(line.getQuoted()) && line.getUnitPrice() != null)
                .map(RfqQuoteItem::getUnitPrice).min(BigDecimal::compareTo).orElse(null);
        RfqQuoteRound chosenRound = latestSubmittedRound(rfq, chosenSupplierId);
        BigDecimal selectedScore = weightedScore(rfq, chosenRound);
        BigDecimal highestScore = rfq.getQuoteRounds().stream()
                .filter(round -> round.getStatus() == RfqQuoteStatus.SUBMITTED)
                .collect(Collectors.groupingBy(round -> round.getSupplier().getId(),
                        Collectors.maxBy(Comparator.comparing(RfqQuoteRound::getRoundNumber))))
                .values().stream().flatMap(Optional::stream).map(round -> weightedScore(rfq, round))
                .filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
        boolean notLowest = lowest != null && chosen.getUnitPrice().compareTo(lowest) > 0;
        boolean notHighestScoring = highestScore != null
                && (selectedScore == null || selectedScore.compareTo(highestScore) < 0);
        if ((notLowest || notHighestScoring) && (reason == null || reason.isBlank())) {
            throw ErrorEnum.RFQ_WINNER_REASON_REQUIRED.ex();
        }
    }

    private RfqQuoteItem requireSupplierQuote(Rfq rfq, UUID supplierId, UUID itemId) {
        RfqQuoteRound latest = latestSubmittedRound(rfq, supplierId);
        if (latest == null) throw ErrorEnum.RFQ_WINNER_INVALID.ex();
        if (itemId == null) return null;
        return latest.getItems().stream().filter(line -> line.getRfqItem().getId().equals(itemId)
                        && Boolean.TRUE.equals(line.getQuoted()) && line.getUnitPrice() != null)
                .findFirst().orElseThrow(() -> ErrorEnum.RFQ_WINNER_INVALID.ex());
    }

    private RfqQuoteRound latestSubmittedRound(Rfq rfq, UUID supplierId) {
        return rfq.getQuoteRounds().stream()
                .filter(round -> round.getSupplier().getId().equals(supplierId)
                        && round.getStatus() == RfqQuoteStatus.SUBMITTED)
                .max(Comparator.comparing(RfqQuoteRound::getRoundNumber))
                .orElseThrow(() -> ErrorEnum.RFQ_WINNER_INVALID.ex());
    }

    private SupplierEntity requireSupplierWithQuote(Rfq rfq, UUID supplierId) {
        boolean invited = rfq.getInvitedSuppliers().stream()
                .anyMatch(invite -> invite.getSupplier().getId().equals(supplierId)
                        && invite.getStatus() != RfqInviteStatus.REVOKED);
        if (!invited || rfq.getQuoteRounds().stream().noneMatch(round -> round.getSupplier().getId().equals(supplierId)
                && round.getStatus() == RfqQuoteStatus.SUBMITTED)) {
            throw ErrorEnum.RFQ_WINNER_INVALID.ex();
        }
        return rfq.getInvitedSuppliers().stream().filter(i -> i.getSupplier().getId().equals(supplierId))
                .findFirst().orElseThrow(() -> ErrorEnum.RFQ_WINNER_INVALID.ex()).getSupplier();
    }

    private void upsertAward(Rfq rfq, RfqItem item, SupplierEntity supplier, String reason,
                             String username, LocalDateTime now, Map<UUID, RfqItemAward> awards) {
        RfqItemAward award = awards.get(item.getId());
        if (award == null) {
            award = RfqItemAward.builder().rfq(rfq).rfqItem(item).supplier(supplier).build();
            rfq.getItemAwards().add(award);
            awards.put(item.getId(), award);
        }
        award.setSupplier(supplier);
        award.setReason(reason);
        award.setSelectedBy(username);
        award.setSelectedAt(now);
    }

    private void revokeTokens(Rfq rfq) {
        rfq.getInvitedSuppliers().forEach(invite -> invitationTokenRepository.revokeActiveTokens(invite.getId()));
    }

    private BigDecimal estimate(List<RfqItem> items) {
        return items.stream().map(item -> item.getBudgetUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addAudit(Rfq rfq, String username, String action, Map<String, Object> details) {
        RfqAuditLog auditLog = new RfqAuditLog(
                rfq, username, action, details, LocalDateTime.now());
        auditLogRepository.save(auditLog);
        rfq.getAuditLogs().add(auditLog);
    }

    private Rfq getEntity(UUID id) {
        return rfqRepository.findById(id).orElseThrow(() -> ErrorEnum.RFQ_NOT_FOUND.ex());
    }

    private void requireOwner(Rfq rfq, String username, boolean isAdmin) {
        if (!isAdmin && !Objects.equals(rfq.getCreatedBy(), username)) throw ErrorEnum.ACCESS_DENIED.ex();
    }

    private String generateRfqCode() {
        return "RFQ-" + Year.now().getValue() + "-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimToNull(String value) { return clean(value); }

    private RfqSummaryResponse toSummary(Rfq rfq) {
        int invited = rfq.getInvitedSuppliers().size();
        int responded = (int) rfq.getInvitedSuppliers().stream()
                .filter(i -> i.getStatus() == RfqInviteStatus.RESPONDED).count();
        String creatorName = creatorName(rfq);
        return new RfqSummaryResponse(rfq.getId(), rfq.getRfqCode(), rfq.getTitle(),
                rfq.getProcurementTicket().getId(), rfq.getProcurementTicket().getTicketCode(),
                rfq.getDepartment(), responded, invited, rfq.getDeadline(), rfq.getEstimatedValue(),
                rfq.getStatus(), rfq.getCreatedBy(), creatorName, rfq.getCreatedAt());
    }

    private RfqDetailResponse toDetail(Rfq rfq) {
        List<ItemResponse> items = rfq.getItems().stream().map(i -> new ItemResponse(i.getId(),
                i.getProcurementItem() == null ? null : i.getProcurementItem().getId(), i.getSku(),
                i.getItemName(), i.getCategory(), i.getQuantity(), i.getUnit(), i.getBudgetUnitPrice(),
                Boolean.TRUE.equals(i.getActive()))).toList();

        List<InvitedSupplierResponse> invites = rfq.getInvitedSuppliers().stream().map(i ->
                new InvitedSupplierResponse(i.getSupplier().getId(), i.getSupplier().getCode(),
                        i.getSupplier().getName(), i.getStatus(), i.getInvitedAt(), i.getRespondedAt())).toList();

        List<ScoreCriterionResponse> criteria = rfq.getScoreCriteria().stream()
                .map(c -> new ScoreCriterionResponse(c.getCriterion(), c.getWeightPercent())).toList();

        List<RfqQuoteRoundResponse> rounds = rankedQuoteResponses(rfq);

        List<ItemAwardResponse> awards = rfq.getItemAwards().stream().map(a -> new ItemAwardResponse(
                a.getRfqItem().getId(), a.getSupplier().getId(), a.getSupplier().getName(), a.getReason(),
                a.getSelectedBy(), a.getSelectedAt())).toList();

        List<RfqAuditLogResponse> history = rfq.getAuditLogs().stream()
                .sorted(Comparator.comparing(
                        RfqAuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> new RfqAuditLogResponse(a.getId(), a.getActorUsername(), a.getAction(),
                        a.getDetails(), a.getCreatedAt())).toList();
        ProcurementTicket ticket = rfq.getProcurementTicket();

        return new RfqDetailResponse(rfq.getId(), rfq.getRfqCode(), rfq.getTitle(), ticket.getId(),
                ticket.getTicketCode(), ticket.getTitle(), rfq.getDepartment(), rfq.getStatus(),
                rfq.getDeadline(), rfq.getQuoteValidityDays(), rfq.getDeliveryLocation(),
                rfq.getDeliveryWindow(), rfq.getCurrency(), rfq.getEstimatedValue(), rfq.getCreatedBy(),
                creatorName(rfq),
                rfq.getCreatedAt(), rfq.getSubmittedAt(), rfq.getReviewedBy(), rfq.getReviewedAt(),
                rfq.getReviewReason(), rfq.getSelectedSupplier() == null ? null : rfq.getSelectedSupplier().getId(),
                rfq.getSelectedSupplier() == null ? null : rfq.getSelectedSupplier().getName(),
                rfq.getSelectionReason(), rfq.getExceptionReason(), items, invites, criteria, rounds, awards, history);
    }

    private String creatorName(Rfq rfq) {
        return userRepository.findByUsername(rfq.getCreatedBy())
                .map(user -> user.getFullName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(rfq.getCreatedBy());
    }

    private List<RfqQuoteRoundResponse> rankedQuoteResponses(Rfq rfq) {
        List<RfqQuoteRoundResponse> mapped = rfq.getQuoteRounds().stream()
                .map(quoteMapper::toQuoteResponse).toList();
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (RfqQuoteRound round : rfq.getQuoteRounds()) {
            BigDecimal score = weightedScore(rfq, round);
            if (score != null) totals.put(round.getId(), score);
        }
        List<UUID> rankedIds = rfq.getQuoteRounds().stream()
                .filter(round -> totals.containsKey(round.getId()))
                .sorted(Comparator.comparing((RfqQuoteRound round) -> totals.get(round.getId())).reversed())
                .map(RfqQuoteRound::getId).toList();
        Map<UUID, Integer> ranks = new HashMap<>();
        for (int i = 0; i < rankedIds.size(); i++) ranks.putIfAbsent(rankedIds.get(i), i + 1);

        List<RfqQuoteRoundResponse> result = new ArrayList<>(mapped.size());
        for (RfqQuoteRoundResponse response : mapped) {
            BigDecimal total = totals.get(response.id());
            result.add(new RfqQuoteRoundResponse(response.id(), response.supplierId(), response.supplierCode(),
                    response.supplierName(), response.roundNumber(), response.status(), response.submittedAt(),
                    response.validUntil(), response.deliveryDays(), response.paymentTerms(), response.warranty(),
                    response.technicalStatus(), response.subtotalBeforeTax(), response.taxAmount(),
                    response.shippingCost(), response.otherCost(), response.totalAfterTax(), total,
                    ranks.get(response.id()), response.incomplete(), response.items(), response.scores()));
        }
        return result;
    }

    private BigDecimal weightedScore(Rfq rfq, RfqQuoteRound round) {
        if (rfq.getScoreCriteria().isEmpty() || round.getScores().isEmpty()) return null;
        Map<RfqScoringCriterion, BigDecimal> scores = round.getScores().stream()
                .collect(Collectors.toMap(RfqQuoteScore::getCriterion, RfqQuoteScore::getScore, (a, b) -> b));
        BigDecimal weighted = BigDecimal.ZERO;
        for (RfqScoreCriterion criterion : rfq.getScoreCriteria()) {
            BigDecimal score = scores.get(criterion.getCriterion());
            if (score == null) return null;
            weighted = weighted.add(score.multiply(criterion.getWeightPercent())
                    .divide(ONE_HUNDRED, 4, java.math.RoundingMode.HALF_UP));
        }
        return weighted.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
