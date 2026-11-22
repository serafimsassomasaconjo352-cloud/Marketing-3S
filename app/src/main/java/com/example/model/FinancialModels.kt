package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

inline fun <reified T : Enum<T>> safeEnumValueOf(raw: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default

enum class TransactionType(val labelPt: String, val isInflow: Boolean) {
    REVENUE("Receita Empresarial", true),
    DIRECT_COST("Custo Direto", false),
    OPERATIONAL_EXPENSE("Despesa Operacional", false),
    DEBT_PAYMENT("Dívida / Pagamento", false),
    RESERVE_CONTRIBUTION("Aporte Reserva Emergência", false),
    PERSONAL_WITHDRAWAL("Retirada Pessoal / Sócio", false)
}

enum class RiskLevel(val labelPt: String) {
    LOW("Risco Baixo"),
    MEDIUM("Risco Médio"),
    HIGH("Risco Alto")
}

enum class InvestmentFocus(val labelPt: String) {
    RECURRING_REVENUE("Receita Recorrente"),
    MARGIN_OPTIMIZATION("Melhoria de Margem"),
    PARETO_80_20("Alavanca 80/20"),
    INFRASTRUCTURE("Infraestrutura & Equipa")
}

enum class InvestmentStatus(val labelPt: String) {
    PROPOSED("Em Análise"),
    ACTIVE("Ativo"),
    COMPLETED("Concluído"),
    DISCARDED("Descartado")
}

enum class ReportPeriodType(val labelPt: String) {
    DAILY("Diário"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    ANNUAL("Anual")
}

@Keep
@IgnoreExtraProperties
data class FinancialPlan(
    @DocumentId val id: String = "financial_plan",
    val companyName: String = "RUMO 3S IA",
    val monthlyRevenueGoalKz: Double = 0.0,
    val monthlyExpenseBudgetKz: Double = 0.0,
    val annualRevenueGoalKz: Double = 0.0,
    val emergencyReserveTargetKz: Double = 0.0,
    val emergencyReserveBalanceKz: Double = 0.0,
    val openingCapitalKz: Double = 0.0,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinancialTransaction(
    @DocumentId val id: String = "",
    val title: String = "",
    val amountKz: Double = 0.0,
    val type: String = TransactionType.REVENUE.name,
    val category: String = "",
    val notes: String? = null,
    val transactionDateMillis: Long = System.currentTimeMillis(),
    val recurring: Boolean = false,
    val settled: Boolean = true,
    val wastefulFlag: Boolean = false,
    val personalSeparated: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedType: TransactionType
        get() = safeEnumValueOf(type, TransactionType.OPERATIONAL_EXPENSE)
}

@Keep
@IgnoreExtraProperties
data class InvestmentItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val capitalKz: Double = 0.0,
    val expectedReturnKz: Double = 0.0,
    val actualReturnKz: Double = 0.0,
    val riskLevel: String = RiskLevel.MEDIUM.name,
    val focusType: String = InvestmentFocus.PARETO_80_20.name,
    val status: String = InvestmentStatus.PROPOSED.name,
    val pareto8020: Boolean = false,
    val justification: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedRisk: RiskLevel
        get() = safeEnumValueOf(riskLevel, RiskLevel.MEDIUM)
    val resolvedFocus: InvestmentFocus
        get() = safeEnumValueOf(focusType, InvestmentFocus.PARETO_80_20)
    val resolvedStatus: InvestmentStatus
        get() = safeEnumValueOf(status, InvestmentStatus.PROPOSED)

    val projectedRoiPercent: Double
        get() = if (capitalKz > 0.0) ((expectedReturnKz - capitalKz) / capitalKz) * 100.0 else 0.0

    val realizedRoiPercent: Double
        get() = if (capitalKz > 0.0) ((actualReturnKz - capitalKz) / capitalKz) * 100.0 else 0.0
}

@Keep
@IgnoreExtraProperties
data class PeriodReport(
    @DocumentId val id: String = "",
    val periodType: String = ReportPeriodType.MONTHLY.name,
    val periodLabel: String = "",
    val totalRevenueKz: Double = 0.0,
    val totalExpenseKz: Double = 0.0,
    val netProfitKz: Double = 0.0,
    val summary: String = "",
    val problemsIdentified: List<String> = emptyList(),
    val opportunities: List<String> = emptyList(),
    val actionPlan: List<String> = emptyList(),
    val generatedByAi: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedPeriodType: ReportPeriodType
        get() = safeEnumValueOf(periodType, ReportPeriodType.MONTHLY)
}

data class CategoryBreakdown(
    val category: String,
    val totalKz: Double,
    val sharePercent: Double,
    val isRevenue: Boolean
)

data class DashboardMetrics(
    val hasPlanConfigured: Boolean = false,
    val availableCapitalKz: Double = 0.0,
    val dailyRevenueKz: Double = 0.0,
    val weeklyRevenueKz: Double = 0.0,
    val monthlyRevenueKz: Double = 0.0,
    val annualRevenueKz: Double = 0.0,
    val monthlyDirectCostsKz: Double = 0.0,
    val monthlyOperationalCostsKz: Double = 0.0,
    val totalMonthlyExpensesKz: Double = 0.0,
    val grossProfitKz: Double = 0.0,
    val netProfitKz: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val activeDebtsKz: Double = 0.0,
    val settledDebtPaymentsKz: Double = 0.0,
    val emergencyReserveKz: Double = 0.0,
    val emergencyReserveTargetKz: Double = 0.0,
    val totalInvestmentsCapitalKz: Double = 0.0,
    val activeInvestmentsCount: Int = 0,
    val averageRoiPercent: Double = 0.0,
    val recurringRevenueKz: Double = 0.0,
    val wastefulSpendingKz: Double = 0.0,
    val personalWithdrawalsKz: Double = 0.0,
    val personalMixAlertsCount: Int = 0,
    val monthlyRevenueGoalKz: Double = 0.0,
    val monthlyExpenseBudgetKz: Double = 0.0,
    val annualRevenueGoalKz: Double = 0.0,
    val capitalNeedsForecastKz: Double = 0.0,
    val runwayMonths: Double = 0.0,
    val topCategories: List<CategoryBreakdown> = emptyList(),
    val dailyCashflowBars: List<DailyCashflowPoint> = emptyList(),
    val monthlyCashflowBars: List<DailyCashflowPoint> = emptyList()
)

data class DailyCashflowPoint(
    val label: String,
    val inflowKz: Double,
    val outflowKz: Double
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CfoChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}
