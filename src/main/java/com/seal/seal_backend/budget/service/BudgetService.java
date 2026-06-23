package com.seal.seal_backend.budget.service;

import com.seal.seal_backend.budget.dto.request.CreateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.request.CreateBudgetRequest;
import com.seal.seal_backend.budget.dto.request.UpdateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.response.BudgetCategoryResponse;
import com.seal.seal_backend.budget.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {
    BudgetResponse createBudget(Long eventId, CreateBudgetRequest req);
    BudgetResponse getBudget(Long eventId);
    BudgetResponse addItem(Long eventId, CreateBudgetItemRequest req);
    BudgetResponse updateItem(Long eventId, Long itemId, UpdateBudgetItemRequest req);
    BudgetResponse deleteItem(Long eventId, Long itemId);
    List<BudgetCategoryResponse> listBudgetCategories();
}
