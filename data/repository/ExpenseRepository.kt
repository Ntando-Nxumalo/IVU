package com.ntando.expensetracker.data.repository

import android.util.Log
import com.ntando.expensetracker.data.dao.CategoryDao
import com.ntando.expensetracker.data.dao.CategorySummary
import com.ntando.expensetracker.data.dao.ExpenseDao
import com.ntando.expensetracker.data.entity.Category
import com.ntando.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to multiple local Room data sources ([ExpenseDao] and [CategoryDao]).
 * It provides a clean API for the UI and ViewModel layers to interact with expense and category data.
 *
 * @property expenseDao DAO handling operations on the [Expense] database table.
 * @property categoryDao DAO handling operations on the [Category] database table.
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    private val TAG = "ExpenseRepository"

    /**
     * Retrieves a reactive stream of all expenses recorded for a specific user.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting a list of [Expense] items.
     */
    fun getAllExpenses(userId: Long): Flow<List<Expense>> {
        Log.d(TAG, "getAllExpenses: Fetching all expenses for userId=$userId")
        return expenseDao.getAllExpenses(userId)
    }

    /**
     * Retrieves a reactive stream of all expense categories associated with a user.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting a list of [Category] items.
     */
    fun getAllCategories(userId: Long): Flow<List<Category>> {
        Log.d(TAG, "getAllCategories: Fetching categories for userId=$userId")
        return categoryDao.getAllCategories(userId)
    }

    /**
     * Retrieves a reactive stream of total aggregated spending amount for a user.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting the sum of expenses as [Double] or `null` if no expenses exist.
     */
    fun getTotalSpending(userId: Long): Flow<Double?> {
        Log.d(TAG, "getTotalSpending: Querying total spending flow for userId=$userId")
        return expenseDao.getTotalSpendingFlow(userId)
    }

    /**
     * Retrieves a reactive stream of expense breakdown summarized by category.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting a list of [CategorySummary] items.
     */
    fun getCategorySummary(userId: Long): Flow<List<CategorySummary>> {
        Log.d(TAG, "getCategorySummary: Querying category spending summary for userId=$userId")
        return expenseDao.getCategorySummary(userId)
    }

    /**
     * Retrieves a reactive stream of recently recorded expenses for a user.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting recent [Expense] records.
     */
    fun getRecentExpenses(userId: Long): Flow<List<Expense>> {
        Log.d(TAG, "getRecentExpenses: Querying recent expenses for userId=$userId")
        return expenseDao.getRecentExpenses(userId)
    }

    /**
     * Retrieves a reactive stream of total expense count recorded for a user.
     *
     * @param userId Unique database identifier of the user.
     * @return [Flow] emitting total count of expenses as [Int].
     */
    fun getExpenseCount(userId: Long): Flow<Int> {
        Log.d(TAG, "getExpenseCount: Querying expense count for userId=$userId")
        return expenseDao.getExpenseCount(userId)
    }

    /**
     * Inserts a new expense record into the local database.
     *
     * @param expense The [Expense] entity to insert.
     */
    suspend fun insertExpense(expense: Expense) {
        Log.d(TAG, "insertExpense: Inserting expense amount=${expense.amount}, categoryId=${expense.categoryId} for userId=${expense.userId}")
        try {
            expenseDao.insertExpense(expense)
            Log.i(TAG, "insertExpense: Expense successfully inserted for userId=${expense.userId}")
        } catch (e: Exception) {
            Log.e(TAG, "insertExpense: Failed to insert expense for userId=${expense.userId}", e)
            throw e
        }
    }

    /**
     * Deletes an existing expense record from the local database.
     *
     * @param expense The [Expense] entity to delete.
     */
    suspend fun deleteExpense(expense: Expense) {
        Log.d(TAG, "deleteExpense: Deleting expense ID=${expense.id}")
        try {
            expenseDao.deleteExpense(expense)
            Log.i(TAG, "deleteExpense: Expense ID=${expense.id} deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "deleteExpense: Failed to delete expense ID=${expense.id}", e)
            throw e
        }
    }

    /**
     * Adds a new expense category to the database.
     *
     * @param category The [Category] entity to insert.
     */
    suspend fun insertCategory(category: Category) {
        Log.d(TAG, "insertCategory: Inserting category name='${category.name}'")
        try {
            categoryDao.insertCategory(category)
            Log.i(TAG, "insertCategory: Category '${category.name}' inserted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "insertCategory: Failed to insert category '${category.name}'", e)
            throw e
        }
    }

    /**
     * Retrieves total spending sum within a specified date range for a user.
     *
     * @param userId Unique database identifier of the user.
     * @param startDate Start date of the range (formatted string).
     * @param endDate End date of the range (formatted string).
     * @return [Flow] emitting total spending in range as [Double] or `null`.
     */
    fun getTotalExpensesInRange(userId: Long, startDate: String, endDate: String): Flow<Double?> {
        Log.d(TAG, "getTotalExpensesInRange: Querying total expenses in range $startDate to $endDate for userId=$userId")
        return expenseDao.getTotalExpensesInRange(userId, startDate, endDate)
    }

    /**
     * Retrieves category-wise expense summaries within a specified date range.
     *
     * @param userId Unique database identifier of the user.
     * @param startDate Start date of the range (formatted string).
     * @param endDate End date of the range (formatted string).
     * @return [Flow] emitting a list of [CategorySummary] items for the range.
     */
    fun getCategorySummaryInRange(userId: Long, startDate: String, endDate: String): Flow<List<CategorySummary>> {
        Log.d(TAG, "getCategorySummaryInRange: Querying category summary in range $startDate to $endDate for userId=$userId")
        return expenseDao.getCategorySummaryInRange(userId, startDate, endDate)
    }
}
