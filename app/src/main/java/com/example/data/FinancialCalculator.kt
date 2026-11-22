package com.example.data

import com.example.model.CategoryBreakdown
import com.example.model.DailyCashflowPoint
import com.example.model.DashboardMetrics
import com.example.model.FinancialPlan
import com.example.model.FinancialTransaction
import com.example.model.InvestmentItem
import com.example.model.InvestmentStatus
import com.example.model.ReportPeriodType
import com.example.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinancialCalculator {

    fun computeMetrics(
        plan: FinancialPlan?,
        transactions: List<FinancialTransaction>,
        investments: List<InvestmentItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardMetrics {
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val startOfDay = (calNow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = (calNow.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = (calNow.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYear = (calNow.clone() as Calendar).apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var settledInflows = 0.0
        var settledBusinessOutflows = 0.0
        var settledReserveContributions = 0.0
        var settledPersonalWithdrawals = 0.0
        var settledDebtPayments = 0.0
        var activeDebts = 0.0

        var dailyRevenue = 0.0
        var weeklyRevenue = 0.0
        var monthlyRevenue = 0.0
        var annualRevenue = 0.0
        var recurringRevenue = 0.0

        var monthlyDirectCosts = 0.0
        var monthlyOpExpenses = 0.0
        var wastefulSpending = 0.0
        var personalMixAlerts = 0

        for (tx in transactions) {
            val type = tx.resolvedType
            val dateMs = tx.transactionDateMillis
            val inDay = dateMs >= startOfDay
            val inWeek = dateMs >= startOfWeek
            val inMonth = dateMs >= startOfMonth
            val inYear = dateMs >= startOfYear

            if (!tx.personalSeparated || type == TransactionType.PERSONAL_WITHDRAWAL) {
                if (!tx.personalSeparated) personalMixAlerts++
                if (type == TransactionType.PERSONAL_WITHDRAWAL && tx.settled) {
                    settledPersonalWithdrawals += tx.amountKz
                }
            }

            if (tx.wastefulFlag) {
                wastefulSpending += tx.amountKz
            }

            when (type) {
                TransactionType.REVENUE -> {
                    if (tx.settled) {
                        settledInflows += tx.amountKz
                        if (inDay) dailyRevenue += tx.amountKz
                        if (inWeek) weeklyRevenue += tx.amountKz
                        if (inMonth) monthlyRevenue += tx.amountKz
                        if (inYear) annualRevenue += tx.amountKz
                        if (tx.recurring && inMonth) recurringRevenue += tx.amountKz
                    }
                }
                TransactionType.DIRECT_COST -> {
                    if (tx.settled) {
                        settledBusinessOutflows += tx.amountKz
                        if (inMonth) monthlyDirectCosts += tx.amountKz
                    } else {
                        activeDebts += tx.amountKz
                    }
                }
                TransactionType.OPERATIONAL_EXPENSE -> {
                    if (tx.settled) {
                        settledBusinessOutflows += tx.amountKz
                        if (inMonth) monthlyOpExpenses += tx.amountKz
                    } else {
                        activeDebts += tx.amountKz
                    }
                }
                TransactionType.DEBT_PAYMENT -> {
                    if (tx.settled) {
                        settledBusinessOutflows += tx.amountKz
                        settledDebtPayments += tx.amountKz
                    } else {
                        activeDebts += tx.amountKz
                    }
                }
                TransactionType.RESERVE_CONTRIBUTION -> {
                    if (tx.settled) {
                        settledReserveContributions += tx.amountKz
                    }
                }
                TransactionType.PERSONAL_WITHDRAWAL -> {
                    // Handled above for separation tracking
                }
            }
        }

        val openingCapital = plan?.openingCapitalKz ?: 0.0
        val availableCapital = (openingCapital + settledInflows - settledBusinessOutflows -
            settledReserveContributions - settledPersonalWithdrawals)

        val grossProfit = monthlyRevenue - monthlyDirectCosts
        val netProfit = grossProfit - monthlyOpExpenses
        val profitMargin = if (monthlyRevenue > 0.0) (netProfit / monthlyRevenue) * 100.0 else 0.0

        val emergencyReserve = (plan?.emergencyReserveBalanceKz ?: 0.0) + settledReserveContributions
        val emergencyTarget = plan?.emergencyReserveTargetKz ?: 0.0

        val activeOrDoneInvestments = investments.filter {
            it.resolvedStatus == InvestmentStatus.ACTIVE || it.resolvedStatus == InvestmentStatus.COMPLETED
        }
        val totalInvCapital = activeOrDoneInvestments.sumOf { it.capitalKz }
        val totalInvReturn = activeOrDoneInvestments.sumOf {
            if (it.actualReturnKz > 0.0) it.actualReturnKz else it.expectedReturnKz
        }
        val averageRoi = if (totalInvCapital > 0.0) {
            ((totalInvReturn - totalInvCapital) / totalInvCapital) * 100.0
        } else 0.0

        val monthlyExpenseBudget = plan?.monthlyExpenseBudgetKz ?: 0.0
        val remainingPlannedExpenses = (monthlyExpenseBudget - (monthlyDirectCosts + monthlyOpExpenses))
            .coerceAtLeast(0.0)
        val requiredLiquidity = activeDebts + remainingPlannedExpenses
        val capitalNeedsForecast = (requiredLiquidity - availableCapital).coerceAtLeast(0.0)

        val monthlyBurn = monthlyDirectCosts + monthlyOpExpenses
        val runwayMonths = if (monthlyBurn > 0.0) emergencyReserve / monthlyBurn else 0.0

        // Top categories breakdown (monthly)
        val monthTxs = transactions.filter { it.transactionDateMillis >= startOfMonth && it.settled }
        val totalMonthVolume = monthTxs.sumOf { it.amountKz }.coerceAtLeast(1.0)
        val topCategories = monthTxs
            .groupBy { it.category.ifBlank { "Geral" } to (it.resolvedType == TransactionType.REVENUE) }
            .map { (key, list) ->
                val sum = list.sumOf { it.amountKz }
                CategoryBreakdown(
                    category = key.first,
                    totalKz = sum,
                    sharePercent = (sum / totalMonthVolume) * 100.0,
                    isRevenue = key.second
                )
            }
            .sortedByDescending { it.totalKz }
            .take(5)

        // Last 7 days cashflow chart points
        val dayFmt = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-AO"))
        val dailyBars = (6 downTo 0).map { offset ->
            val dayStart = (calNow.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val dayTxs = transactions.filter {
                it.settled && it.transactionDateMillis in dayStart until dayEnd
            }
            val inflows = dayTxs.filter { it.resolvedType == TransactionType.REVENUE }.sumOf { it.amountKz }
            val outflows = dayTxs.filter { it.resolvedType != TransactionType.REVENUE }.sumOf { it.amountKz }
            DailyCashflowPoint(
                label = dayFmt.format(Date(dayStart)),
                inflowKz = inflows,
                outflowKz = outflows
            )
        }

        // Last 6 months cashflow chart points
        val monthFmt = SimpleDateFormat("MMM", Locale.forLanguageTag("pt-AO"))
        val monthlyBars = (5 downTo 0).map { offset ->
            val monthCal = (calNow.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStart = monthCal.timeInMillis
            val nextMonthCal = (monthCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            val monthEnd = nextMonthCal.timeInMillis
            val monthTxs = transactions.filter {
                it.settled && it.transactionDateMillis in monthStart until monthEnd
            }
            val inflows = monthTxs.filter { it.resolvedType == TransactionType.REVENUE }.sumOf { it.amountKz }
            val outflows = monthTxs.filter { it.resolvedType != TransactionType.REVENUE }.sumOf { it.amountKz }
            val rawLabel = monthFmt.format(Date(monthStart)).replace(".", "")
            val label = rawLabel.replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-AO")) }
            DailyCashflowPoint(
                label = label,
                inflowKz = inflows,
                outflowKz = outflows
            )
        }

        return DashboardMetrics(
            hasPlanConfigured = plan != null,
            availableCapitalKz = availableCapital,
            dailyRevenueKz = dailyRevenue,
            weeklyRevenueKz = weeklyRevenue,
            monthlyRevenueKz = monthlyRevenue,
            annualRevenueKz = annualRevenue,
            monthlyDirectCostsKz = monthlyDirectCosts,
            monthlyOperationalCostsKz = monthlyOpExpenses,
            totalMonthlyExpensesKz = monthlyBurn,
            grossProfitKz = grossProfit,
            netProfitKz = netProfit,
            profitMarginPercent = profitMargin,
            activeDebtsKz = activeDebts,
            settledDebtPaymentsKz = settledDebtPayments,
            emergencyReserveKz = emergencyReserve,
            emergencyReserveTargetKz = emergencyTarget,
            totalInvestmentsCapitalKz = totalInvCapital,
            activeInvestmentsCount = activeOrDoneInvestments.size,
            averageRoiPercent = averageRoi,
            recurringRevenueKz = recurringRevenue,
            wastefulSpendingKz = wastefulSpending,
            personalWithdrawalsKz = settledPersonalWithdrawals,
            personalMixAlertsCount = personalMixAlerts,
            monthlyRevenueGoalKz = plan?.monthlyRevenueGoalKz ?: 0.0,
            monthlyExpenseBudgetKz = monthlyExpenseBudget,
            annualRevenueGoalKz = plan?.annualRevenueGoalKz ?: 0.0,
            capitalNeedsForecastKz = capitalNeedsForecast,
            runwayMonths = runwayMonths,
            topCategories = topCategories,
            dailyCashflowBars = dailyBars,
            monthlyCashflowBars = monthlyBars
        )
    }

    fun periodTotals(
        periodType: ReportPeriodType,
        transactions: List<FinancialTransaction>,
        nowMillis: Long = System.currentTimeMillis()
    ): Triple<Double, Double, Double> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        when (periodType) {
            ReportPeriodType.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            ReportPeriodType.WEEKLY -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            ReportPeriodType.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            ReportPeriodType.ANNUAL -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        }
        val cutoff = cal.timeInMillis
        val scoped = transactions.filter { it.settled && it.transactionDateMillis >= cutoff }
        val rev = scoped.filter { it.resolvedType == TransactionType.REVENUE }.sumOf { it.amountKz }
        val exp = scoped.filter {
            it.resolvedType == TransactionType.DIRECT_COST ||
                it.resolvedType == TransactionType.OPERATIONAL_EXPENSE ||
                it.resolvedType == TransactionType.DEBT_PAYMENT
        }.sumOf { it.amountKz }
        return Triple(rev, exp, rev - exp)
    }
}
