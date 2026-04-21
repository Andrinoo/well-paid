package com.wellpaid.navigation

import android.net.Uri

object NavRoutes {
    const val Login = "login"
    const val Main = "main"
    const val Settings = "settings"
    const val ManageCategories = "manage_categories"
    const val Security = "security"
    const val DisplayName = "display_name"
    const val Family = "family"
    const val Register = "register"
    private const val VERIFY_EMAIL = "verify_email"

    const val VerifyEmailRoute = "$VERIFY_EMAIL/{email}"

    fun verifyEmail(email: String): String =
        "$VERIFY_EMAIL/${Uri.encode(email)}"

    const val ForgotPassword = "forgot_password"

    private const val RESET_PASSWORD = "reset_password"
    const val ResetPasswordRoute = "$RESET_PASSWORD?token={token}"

    fun resetPassword(token: String = ""): String =
        "$RESET_PASSWORD?token=${Uri.encode(token)}"

    const val ExpenseNew = "expense_new"

    private const val EXPENSE_DETAIL = "expense"
    const val ExpenseDetailRoute = "$EXPENSE_DETAIL/{expenseId}"

    fun expenseDetail(expenseId: String): String = "$EXPENSE_DETAIL/$expenseId"

    private const val INSTALLMENT_PLAN = "installment_plan"
    const val InstallmentPlanRoute = "$INSTALLMENT_PLAN/{groupId}"

    fun installmentPlan(groupId: String): String = "$INSTALLMENT_PLAN/$groupId"

    const val IncomeNew = "income_new"

    private const val INCOME_DETAIL = "income"
    const val IncomeDetailRoute = "$INCOME_DETAIL/{incomeId}"

    fun incomeDetail(incomeId: String): String = "$INCOME_DETAIL/$incomeId"

    const val GoalNew = "goal_new"

    private const val GOAL_DETAIL = "goal"
    const val GoalDetailRoute = "$GOAL_DETAIL/{goalId}"

    fun goalDetail(goalId: String): String = "$GOAL_DETAIL/$goalId"

    private const val GOAL_EDIT = "goal_edit"
    const val GoalEditRoute = "$GOAL_EDIT/{goalId}"

    fun goalEdit(goalId: String): String = "$GOAL_EDIT/$goalId"

    const val ShoppingLists = "shopping_lists"

    private const val SHOPPING_LIST = "shopping_list"
    const val ShoppingListDetailRoute = "$SHOPPING_LIST/{listId}"

    fun shoppingListDetail(listId: String): String = "$SHOPPING_LIST/$listId"

    const val ShoppingListNew = "shopping_list_new"

    /** Recados / avisos publicados pelo admin. */
    const val Announcements = "announcements"

    /** Valores a receber / a pagar entre membros (despesas partilhadas). */
    const val Receivables = "receivables"

    /** Tela MVP de investimentos. */
    const val Investments = "investments"
}