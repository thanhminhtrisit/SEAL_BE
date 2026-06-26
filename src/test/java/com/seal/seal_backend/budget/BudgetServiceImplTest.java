package com.seal.seal_backend.budget;

import com.seal.seal_backend.budget.dto.request.CreateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.request.CreateBudgetRequest;
import com.seal.seal_backend.budget.dto.request.UpdateBudgetRequest;
import com.seal.seal_backend.domain.enums.BudgetStatus;
import com.seal.seal_backend.budget.dto.response.BudgetResponse;
import com.seal.seal_backend.budget.service.impl.BudgetServiceImpl;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.BudgetStatus;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventBudgetRepository eventBudgetRepository;
    @Mock BudgetItemRepository budgetItemRepository;
    @Mock BudgetCategoryRepository budgetCategoryRepository;
    @Mock EntityManager entityManager;

    @InjectMocks BudgetServiceImpl budgetService;

    private Event event;
    private EventBudget budget;
    private BudgetCategory category;

    @BeforeEach
    void setup() {
        event = new Event();
        event.setId(10L);

        budget = new EventBudget();
        budget.setId(1L);
        budget.setEvent(event);
        budget.setCurrency("VND");
        budget.setStatus(BudgetStatus.DRAFT);
        budget.setTotalEstimatedCost(BigDecimal.ZERO);

        category = new BudgetCategory();
        category.setId(2L);
        category.setCode("PRIZE");
        category.setName("Prize");
    }

    // ─── Pending Lock ─────────────────────────────────────────────────────────

    @Nested
    class PendingLock {

        @Test
        void createBudget_pendingApproval_throws_BR_EVT_07() {
            event.setStatus(EventStatus.PENDING_APPROVAL);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> budgetService.createBudget(10L, new CreateBudgetRequest(null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── Create Budget ────────────────────────────────────────────────────────

    @Nested
    class CreateBudget {

        @Test
        void newBudget_createsWithVndDefault() {
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.empty());
            when(eventBudgetRepository.save(any())).thenReturn(budget);

            BudgetResponse res = budgetService.createBudget(10L, new CreateBudgetRequest(null));

            assertThat(res.currency()).isEqualTo("VND");
            assertThat(res.status()).isEqualTo("DRAFT");
            assertThat(res.items()).isEmpty();
            verify(eventBudgetRepository).save(argThat(b -> "VND".equals(b.getCurrency())));
        }

        @Test
        void duplicateBudget_throws_BR_BGT_01() {
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));

            assertThatThrownBy(() -> budgetService.createBudget(10L, new CreateBudgetRequest(null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-BGT-01");
        }

        @Test
        void eventNotFound_throws_ResourceNotFoundException() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.createBudget(99L, new CreateBudgetRequest(null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Add Item ─────────────────────────────────────────────────────────────

    @Nested
    class AddItem {

        @Test
        void addOneItem_refreshedTotalReturned() {
            BudgetItem saved = buildItem(1L, new BigDecimal("2"), new BigDecimal("100000"),
                    new BigDecimal("200000"));

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(budgetCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(budgetItemRepository.save(any())).thenReturn(saved);
            // Simulate trigger: set total after flush
            doAnswer(inv -> {
                budget.setTotalEstimatedCost(new BigDecimal("200000"));
                return null;
            }).when(entityManager).refresh(budget);
            when(budgetItemRepository.findByBudgetId(1L)).thenReturn(List.of(saved));

            BudgetResponse res = budgetService.addItem(10L,
                    new CreateBudgetItemRequest(2L, "Prize pool", new BigDecimal("2"),
                            new BigDecimal("100000"), null));

            assertThat(res.totalEstimatedCost()).isEqualByComparingTo("200000");
            assertThat(res.items()).hasSize(1);
            verify(entityManager).flush();
            verify(entityManager).refresh(budget);
        }

        @Test
        void addTwoItems_totalEqualsSumOfAmounts() {
            BudgetItem item1 = buildItem(1L, new BigDecimal("2"), new BigDecimal("100000"),
                    new BigDecimal("200000"));
            BudgetItem item2 = buildItem(2L, new BigDecimal("3"), new BigDecimal("50000"),
                    new BigDecimal("150000"));

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(budgetCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(budgetItemRepository.save(any())).thenReturn(item1).thenReturn(item2);

            // First add: total becomes 200000
            doAnswer(inv -> {
                budget.setTotalEstimatedCost(new BigDecimal("200000"));
                return null;
            }).doAnswer(inv -> {
                // Second add: total becomes 350000
                budget.setTotalEstimatedCost(new BigDecimal("350000"));
                return null;
            }).when(entityManager).refresh(budget);

            when(budgetItemRepository.findByBudgetId(1L))
                    .thenReturn(List.of(item1))
                    .thenReturn(List.of(item1, item2));

            budgetService.addItem(10L, new CreateBudgetItemRequest(2L, "Item1",
                    new BigDecimal("2"), new BigDecimal("100000"), null));

            BudgetResponse res = budgetService.addItem(10L, new CreateBudgetItemRequest(2L, "Item2",
                    new BigDecimal("3"), new BigDecimal("50000"), null));

            assertThat(res.totalEstimatedCost()).isEqualByComparingTo("350000");
            assertThat(res.items()).hasSize(2);
        }

        @Test
        void categoryNotFound_throws_ResourceNotFoundException() {
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(budgetCategoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.addItem(10L,
                    new CreateBudgetItemRequest(99L, "desc", BigDecimal.ONE,
                            BigDecimal.ZERO, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Delete Item ──────────────────────────────────────────────────────────

    @Nested
    class DeleteItem {

        @Test
        void deleteItem_totalDecreases() {
            BudgetItem item = buildItem(5L, new BigDecimal("2"), new BigDecimal("100000"),
                    new BigDecimal("200000"));
            budget.setTotalEstimatedCost(new BigDecimal("200000"));

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
            doAnswer(inv -> {
                budget.setTotalEstimatedCost(BigDecimal.ZERO);
                return null;
            }).when(entityManager).refresh(budget);
            when(budgetItemRepository.findByBudgetId(1L)).thenReturn(List.of());

            BudgetResponse res = budgetService.deleteItem(10L, 5L);

            assertThat(res.totalEstimatedCost()).isEqualByComparingTo("0");
            assertThat(res.items()).isEmpty();
            verify(budgetItemRepository).delete(item);
            verify(entityManager).flush();
            verify(entityManager).refresh(budget);
        }

        @Test
        void deleteItemFromDifferentBudget_throws_ForbiddenActionException() {
            // Item belongs to budget id=99, not budget id=1
            EventBudget otherBudget = new EventBudget();
            otherBudget.setId(99L);
            BudgetItem alien = buildItem(5L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
            alien.setBudget(otherBudget);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(alien));

            assertThatThrownBy(() -> budgetService.deleteItem(10L, 5L))
                    .isInstanceOf(ForbiddenActionException.class);
        }
    }

    // ─── Patch Budget Header ──────────────────────────────────────────────────

    @Nested
    class PatchBudget {

        @Test
        void patchCurrency_updatesSuccessfully() {
            event.setStatus(EventStatus.DRAFT);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(eventBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(budgetItemRepository.findByBudgetId(1L)).thenReturn(List.of());

            BudgetResponse res = budgetService.patchBudget(10L, new UpdateBudgetRequest("USD", null));

            assertThat(res.currency()).isEqualTo("USD");
            assertThat(res.status()).isEqualTo("DRAFT");
        }

        @Test
        void patchStatus_updatesSuccessfully() {
            event.setStatus(EventStatus.DRAFT);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(eventBudgetRepository.findByEventId(10L)).thenReturn(Optional.of(budget));
            when(eventBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(budgetItemRepository.findByBudgetId(1L)).thenReturn(List.of());

            BudgetResponse res = budgetService.patchBudget(10L,
                    new UpdateBudgetRequest(null, BudgetStatus.PENDING_APPROVAL));

            assertThat(res.status()).isEqualTo("PENDING_APPROVAL");
        }

        @Test
        void patchBudget_pendingApprovalEvent_throws_BR_EVT_07() {
            event.setStatus(EventStatus.PENDING_APPROVAL);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> budgetService.patchBudget(10L,
                    new UpdateBudgetRequest("USD", null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BudgetItem buildItem(Long id, BigDecimal qty, BigDecimal unit, BigDecimal amount) {
        BudgetItem item = new BudgetItem();
        item.setId(id);
        item.setBudget(budget);
        item.setCategory(category);
        item.setDescription("desc");
        item.setQuantity(qty);
        item.setUnitCost(unit);
        item.setAmount(amount);
        return item;
    }
}
