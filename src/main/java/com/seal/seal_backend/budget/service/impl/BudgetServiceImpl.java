package com.seal.seal_backend.budget.service.impl;

import com.seal.seal_backend.budget.dto.request.CreateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.request.CreateBudgetRequest;
import com.seal.seal_backend.budget.dto.request.UpdateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.response.BudgetCategoryResponse;
import com.seal.seal_backend.budget.dto.response.BudgetItemResponse;
import com.seal.seal_backend.budget.dto.response.BudgetResponse;
import com.seal.seal_backend.budget.service.BudgetService;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.BudgetStatus;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final EventRepository eventRepository;
    private final EventBudgetRepository eventBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final EntityManager entityManager;

    // ─── FR-BGT-01: Create budget ─────────────────────────────────────────────

    @Override
    @Transactional
    public BudgetResponse createBudget(Long eventId, CreateBudgetRequest req) {
        Event event = findEvent(eventId);
        validateNotPending(event);

        if (eventBudgetRepository.findByEventId(eventId).isPresent()) {
            throw new BusinessRuleException("BR-BGT-01",
                    "Event " + eventId + " already has a budget");
        }

        EventBudget budget = new EventBudget();
        budget.setEvent(event);
        budget.setCurrency(req.currency() != null && !req.currency().isBlank()
                ? req.currency() : "VND");
        budget.setStatus(BudgetStatus.DRAFT);
        budget = eventBudgetRepository.save(budget);

        return BudgetResponse.from(budget, List.of());
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(Long eventId) {
        EventBudget budget = findBudgetForEvent(eventId);
        List<BudgetItemResponse> items = budgetItemRepository.findByBudgetId(budget.getId())
                .stream().map(BudgetItemResponse::from).toList();
        return BudgetResponse.from(budget, items);
    }

    // ─── FR-BGT-02: Add item ──────────────────────────────────────────────────

    @Override
    @Transactional
    public BudgetResponse addItem(Long eventId, CreateBudgetItemRequest req) {
        validateNotPending(findEvent(eventId));
        EventBudget budget = findBudgetForEvent(eventId);
        BudgetCategory category = findCategory(req.categoryId());

        BudgetItem item = new BudgetItem();
        item.setBudget(budget);
        item.setCategory(category);
        item.setDescription(req.description());
        item.setQuantity(req.quantity());
        item.setUnitCost(req.unitCost());
        item.setNotes(req.notes());
        budgetItemRepository.save(item);

        // Flush so DB trigger fires and updates total_estimated_cost, then refresh entity
        entityManager.flush();
        entityManager.refresh(budget);

        return buildResponse(budget);
    }

    // ─── FR-BGT-03: Update item ───────────────────────────────────────────────

    @Override
    @Transactional
    public BudgetResponse updateItem(Long eventId, Long itemId, UpdateBudgetItemRequest req) {
        validateNotPending(findEvent(eventId));
        EventBudget budget = findBudgetForEvent(eventId);
        BudgetItem item = findItem(itemId, budget.getId());
        BudgetCategory category = findCategory(req.categoryId());

        item.setCategory(category);
        item.setDescription(req.description());
        item.setQuantity(req.quantity());
        item.setUnitCost(req.unitCost());
        item.setNotes(req.notes());
        budgetItemRepository.save(item);

        entityManager.flush();
        entityManager.refresh(budget);

        return buildResponse(budget);
    }

    // ─── FR-BGT-04: Delete item ───────────────────────────────────────────────

    @Override
    @Transactional
    public BudgetResponse deleteItem(Long eventId, Long itemId) {
        validateNotPending(findEvent(eventId));
        EventBudget budget = findBudgetForEvent(eventId);
        BudgetItem item = findItem(itemId, budget.getId());

        budgetItemRepository.delete(item);

        entityManager.flush();
        entityManager.refresh(budget);

        return buildResponse(budget);
    }

    // ─── Budget Categories ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<BudgetCategoryResponse> listBudgetCategories() {
        return budgetCategoryRepository.findAllByOrderByNameAsc()
                .stream().map(BudgetCategoryResponse::from).toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private BudgetResponse buildResponse(EventBudget budget) {
        List<BudgetItemResponse> items = budgetItemRepository.findByBudgetId(budget.getId())
                .stream().map(BudgetItemResponse::from).toList();
        return BudgetResponse.from(budget, items);
    }

    private EventBudget findBudgetForEvent(Long eventId) {
        findEvent(eventId);
        return eventBudgetRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No budget found for event " + eventId));
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    private void validateNotPending(Event event) {
        if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("BR-EVT-07",
                    "Event " + event.getId() + " is locked for editing while pending approval.");
        }
    }

    private BudgetCategory findCategory(Long categoryId) {
        return budgetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BudgetCategory not found: " + categoryId));
    }

    private BudgetItem findItem(Long itemId, Long budgetId) {
        BudgetItem item = budgetItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetItem not found: " + itemId));
        if (!item.getBudget().getId().equals(budgetId)) {
            throw new ForbiddenActionException(
                    "Item " + itemId + " does not belong to this event's budget");
        }
        return item;
    }
}
