package com.example

import com.example.data.FinancialCalculator
import com.example.model.FinancialPlan
import com.example.model.FinancialTransaction
import com.example.model.InvestmentItem
import com.example.model.InvestmentStatus
import com.example.model.ReportPeriodType
import com.example.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun `computes revenue vs profit, capital, debts, reserve, and ROI accurately in Kwanzas`() {
        val now = 1_795_195_000_000L
        val plan = FinancialPlan(
            companyName = "RUMO 3S IA",
            openingCapitalKz = 1_000_000.0,
            monthlyRevenueGoalKz = 2_000_000.0,
            monthlyExpenseBudgetKz = 800_000.0,
            annualRevenueGoalKz = 24_000_000.0,
            emergencyReserveBalanceKz = 500_000.0,
            emergencyReserveTargetKz = 3_000_000.0
        )

        val transactions = listOf(
            FinancialTransaction(
                id = "t1",
                title = "Contrato IA B2B",
                amountKz = 1_200_000.0,
                type = TransactionType.REVENUE.name,
                category = "Serviços IA",
                transactionDateMillis = now - 3_600_000L,
                recurring = true,
                settled = true,
                wastefulFlag = false,
                personalSeparated = true
            ),
            FinancialTransaction(
                id = "t2",
                title = "API Cloud & GPU",
                amountKz = 200_000.0,
                type = TransactionType.DIRECT_COST.name,
                category = "Infraestrutura",
                transactionDateMillis = now - 3_600_000L,
                settled = true
            ),
            FinancialTransaction(
                id = "t3",
                title = "Internet & Energia",
                amountKz = 150_000.0,
                type = TransactionType.OPERATIONAL_EXPENSE.name,
                category = "Energia/Internet",
                transactionDateMillis = now - 3_600_000L,
                settled = true
            ),
            FinancialTransaction(
                id = "t4",
                title = "Fornecedor Pendente",
                amountKz = 100_000.0,
                type = TransactionType.DEBT_PAYMENT.name,
                category = "Dívidas",
                transactionDateMillis = now - 3_600_000L,
                settled = false
            ),
            FinancialTransaction(
                id = "t5",
                title = "Aporte Reserva",
                amountKz = 250_000.0,
                type = TransactionType.RESERVE_CONTRIBUTION.name,
                category = "Reserva",
                transactionDateMillis = now - 3_600_000L,
                settled = true
            ),
            FinancialTransaction(
                id = "t6",
                title = "Retirada Pessoal",
                amountKz = 50_000.0,
                type = TransactionType.PERSONAL_WITHDRAWAL.name,
                category = "Pró-labore",
                transactionDateMillis = now - 3_600_000L,
                settled = true,
                personalSeparated = false
            )
        )

        val investments = listOf(
            InvestmentItem(
                id = "inv1",
                title = "Funil WhatsApp IA",
                capitalKz = 200_000.0,
                expectedReturnKz = 500_000.0,
                actualReturnKz = 400_000.0,
                status = InvestmentStatus.ACTIVE.name,
                pareto8020 = true
            )
        )

        val m = FinancialCalculator.computeMetrics(plan, transactions, investments, nowMillis = now)

        // Revenue = 1,200,000; Direct cost = 200,000 -> Gross Profit = 1,000,000
        assertEquals(1_200_000.0, m.monthlyRevenueKz, 0.01)
        assertEquals(1_000_000.0, m.grossProfitKz, 0.01)
        // Net Profit = 1,000,000 - 150,000 = 850,000 (not confused with revenue or withdrawals!)
        assertEquals(850_000.0, m.netProfitKz, 0.01)
        // Available capital = 1,000,000 + 1,200,000 - 200,000 - 150,000 - 250,000 - 50,000 = 1,550,000
        assertEquals(1_550_000.0, m.availableCapitalKz, 0.01)
        // Active debts = 100,000
        assertEquals(100_000.0, m.activeDebtsKz, 0.01)
        // Emergency reserve = 500,000 + 250,000 = 750,000
        assertEquals(750_000.0, m.emergencyReserveKz, 0.01)
        // Total monthly expenses = 200,000 + 150,000 = 350,000
        assertEquals(350_000.0, m.totalMonthlyExpensesKz, 0.01)
        // Total investments capital = 200,000; active count = 1; ROI = 100%
        assertEquals(200_000.0, m.totalInvestmentsCapitalKz, 0.01)
        assertEquals(1, m.activeInvestmentsCount)
        assertEquals(100.0, m.averageRoiPercent, 0.01)
        assertEquals(1, m.personalMixAlertsCount)
        assertTrue(m.profitMarginPercent > 70.0)

        val (pRev, pExp, pNet) = FinancialCalculator.periodTotals(ReportPeriodType.MONTHLY, transactions, nowMillis = now)
        assertEquals(1_200_000.0, pRev, 0.01)
        assertEquals(350_000.0, pExp, 0.01)
        assertEquals(850_000.0, pNet, 0.01)

        assertEquals(6, m.monthlyCashflowBars.size)
        val currentMonthBar = m.monthlyCashflowBars.last()
        assertEquals(1_200_000.0, currentMonthBar.inflowKz, 0.01)
        assertEquals(650_000.0, currentMonthBar.outflowKz, 0.01)
    }
}
