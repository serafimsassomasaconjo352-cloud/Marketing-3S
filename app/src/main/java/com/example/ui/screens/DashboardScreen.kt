package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CategoryBreakdown
import com.example.model.DailyCashflowPoint
import com.example.model.DashboardMetrics
import com.example.model.FinancialPlan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRumoColors

private enum class RevenueWindow(val label: String) {
    DAILY("Diário"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    ANNUAL("Anual")
}

private enum class CashflowChartMode(val label: String) {
    MONTHLY_6M("Mensal (6M)"),
    DAILY_7D("Diário (7D)")
}

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    plan: FinancialPlan?,
    isSavingPlan: Boolean,
    onSavePlan: (FinancialPlan, () -> Unit) -> Unit,
    onNavigateToCashflow: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToAdvisor: () -> Unit,
    onSignOut: () -> Unit
) {
    var showPlanDialog by remember { mutableStateOf(false) }
    var selectedRevenueWindow by remember { mutableStateOf(RevenueWindow.MONTHLY) }
    var selectedChartMode by remember { mutableStateOf(CashflowChartMode.MONTHLY_6M) }
    val rumoColors = LocalRumoColors.current

    if (showPlanDialog) {
        BudgetPlanDialog(
            currentPlan = plan,
            isSaving = isSavingPlan,
            onDismiss = { showPlanDialog = false },
            onSave = { updatedPlan ->
                onSavePlan(updatedPlan) { showPlanDialog = false }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Material 3 Elevated Hero Card — Saldo Atual (Kz)
            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cfo_hero_banner),
                            contentDescription = "Painel Executivo RUMO 3S IA",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            rumoColors.heroDeep.copy(alpha = 0.92f),
                                            rumoColors.heroDeep.copy(alpha = 0.74f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = plan?.companyName?.ifBlank { "RUMO 3S IA" } ?: "RUMO 3S IA",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = rumoColors.heroGold
                                    )
                                    Text(
                                        text = "Saldo Atual • Capital Disponível",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { showPlanDialog = true },
                                        modifier = Modifier.testTag("edit_plan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Configurar Orçamento e Metas",
                                            tint = rumoColors.heroGold
                                        )
                                    }
                                    IconButton(
                                        onClick = onSignOut,
                                        modifier = Modifier.testTag("sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Terminar sessão",
                                            tint = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = Formatters.formatKz(metrics.availableCapitalKz),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("kpi_available_capital")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Lucro Líquido (Mês)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.78f)
                                        )
                                        Text(
                                            text = Formatters.formatKz(metrics.netProfitKz, compact = true),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = if (metrics.netProfitKz >= 0) Color(0xFF7BF2C0) else Color(0xFFFF9C91)
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Margem Líquida",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.78f)
                                        )
                                        Text(
                                            text = Formatters.formatPercent(metrics.profitMarginPercent),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = rumoColors.heroGold
                                        )
                                    }
                                }
                            }

                            // Quick Material 3 Action Row inside Hero
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onNavigateToCashflow,
                                    modifier = Modifier.weight(1f).testTag("hero_quick_cashflow_cta")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Lançar Caixa", maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = onNavigateToAdvisor,
                                    modifier = Modifier.weight(1f).testTag("open_cfo_advisor_cta")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("CFO IA", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Primary Summary Cards: Despesas Mensais (Kz) & Total de Investimentos (Kz)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryMetricCard(
                        title = "Despesas Mensais",
                        value = Formatters.formatKz(metrics.totalMonthlyExpensesKz, compact = true),
                        detail = if (metrics.monthlyExpenseBudgetKz > 0) {
                            "Orçamento: ${Formatters.formatKz(metrics.monthlyExpenseBudgetKz, compact = true)}"
                        } else {
                            "Operac. + Diretos"
                        },
                        icon = Icons.Default.ReceiptLong,
                        accentColor = rumoColors.loss,
                        progress = if (metrics.monthlyExpenseBudgetKz > 0) {
                            (metrics.totalMonthlyExpensesKz / metrics.monthlyExpenseBudgetKz).toFloat().coerceIn(0f, 1f)
                        } else null,
                        onClick = onNavigateToCashflow,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_monthly_expenses_card")
                    )
                    PrimaryMetricCard(
                        title = "Total Investimentos",
                        value = Formatters.formatKz(metrics.totalInvestmentsCapitalKz, compact = true),
                        detail = "${metrics.activeInvestmentsCount} ativo(s) • ROI ${Formatters.formatPercent(metrics.averageRoiPercent)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = rumoColors.heroGold,
                        progress = null,
                        onClick = onNavigateToInvestments,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_investments_card")
                    )
                }
            }

            // First-run Planning Prompt if plan not configured yet
            if (!metrics.hasPlanConfigured) {
                item {
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Configure o Orçamento da RUMO 3S IA",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Define o capital inicial, teto de despesas mensais, metas em Kwanzas e reserva de emergência. Nunca inventamos números: tudo parte dos teus registos reais.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showPlanDialog = true },
                                    modifier = Modifier.testTag("setup_plan_cta")
                                ) {
                                    Text("Definir Metas e Orçamento")
                                }
                                OutlinedButton(onClick = onNavigateToCashflow) {
                                    Text("Registar Movimento")
                                }
                            }
                        }
                    }
                }
            }

            // Governance / Separation & Waste Alert Strip
            if (metrics.personalMixAlertsCount > 0 || metrics.personalWithdrawalsKz > 0 || metrics.wastefulSpendingKz > 0) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta de governação financeira",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Alerta de Disciplina CFO",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (metrics.personalMixAlertsCount > 0 || metrics.personalWithdrawalsKz > 0) {
                                    Text(
                                        text = "Separação Pessoal/Empresa: ${metrics.personalMixAlertsCount} alerta(s), ${Formatters.formatKz(metrics.personalWithdrawalsKz)} retirados.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (metrics.wastefulSpendingKz > 0) {
                                    Text(
                                        text = "Desperdícios sinalizados: ${Formatters.formatKz(metrics.wastefulSpendingKz)} passíveis de corte.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Revenue Switcher using Material 3 SingleChoiceSegmentedButtonRow
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Faturamento (Receita Bruta)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                color = rumoColors.profit.copy(alpha = 0.12f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "Recorrente: ${Formatters.formatKz(metrics.recurringRevenueKz, compact = true)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = rumoColors.profit,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            RevenueWindow.entries.forEachIndexed { index, window ->
                                SegmentedButton(
                                    selected = selectedRevenueWindow == window,
                                    onClick = { selectedRevenueWindow = window },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = RevenueWindow.entries.size
                                    ),
                                    modifier = Modifier.testTag("rev_filter_${window.name.lowercase()}")
                                ) {
                                    Text(window.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        val revenueValue = when (selectedRevenueWindow) {
                            RevenueWindow.DAILY -> metrics.dailyRevenueKz
                            RevenueWindow.WEEKLY -> metrics.weeklyRevenueKz
                            RevenueWindow.MONTHLY -> metrics.monthlyRevenueKz
                            RevenueWindow.ANNUAL -> metrics.annualRevenueKz
                        }
                        Text(
                            text = Formatters.formatKz(revenueValue),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = rumoColors.profit
                        )
                    }
                }
            }

            // 4. Secondary Financial Indicators Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Indicadores de Controlo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMiniCard(
                            title = "Custos Operacionais",
                            value = Formatters.formatKz(metrics.monthlyOperationalCostsKz, compact = true),
                            subtitle = "Diretos: ${Formatters.formatKz(metrics.monthlyDirectCostsKz, compact = true)}",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = rumoColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMiniCard(
                            title = "Lucro Bruto (Mês)",
                            value = Formatters.formatKz(metrics.grossProfitKz, compact = true),
                            subtitle = "Antes de custos fixos",
                            icon = Icons.Default.Assessment,
                            accentColor = if (metrics.grossProfitKz >= 0) rumoColors.profit else rumoColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMiniCard(
                            title = "Dívidas Ativas",
                            value = Formatters.formatKz(metrics.activeDebtsKz, compact = true),
                            subtitle = "Pagas: ${Formatters.formatKz(metrics.settledDebtPaymentsKz, compact = true)}",
                            icon = Icons.Default.AccountBalance,
                            accentColor = if (metrics.activeDebtsKz > 0) rumoColors.warning else rumoColors.profit,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMiniCard(
                            title = "Reserva Emergência",
                            value = Formatters.formatKz(metrics.emergencyReserveKz, compact = true),
                            subtitle = "Cobertura: ${"%.1f".format(metrics.runwayMonths)} meses",
                            icon = Icons.Default.Security,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMiniCard(
                            title = "Necessidade Capital",
                            value = Formatters.formatKz(metrics.capitalNeedsForecastKz, compact = true),
                            subtitle = if (metrics.capitalNeedsForecastKz == 0.0) "Caixa cobre compromissos" else "Défice previsto no mês",
                            icon = Icons.Default.Savings,
                            accentColor = if (metrics.capitalNeedsForecastKz > 0) rumoColors.loss else rumoColors.profit,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMiniCard(
                            title = "Desperdício Sinalizado",
                            value = Formatters.formatKz(metrics.wastefulSpendingKz, compact = true),
                            subtitle = if (metrics.wastefulSpendingKz == 0.0) "Sem desperdícios" else "Rever gastos supérfluos",
                            icon = Icons.Default.Warning,
                            accentColor = if (metrics.wastefulSpendingKz > 0) rumoColors.warning else rumoColors.profit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Monthly & Annual Budget Progress
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Orçamento & Metas",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = { showPlanDialog = true }) {
                                Text("Editar Plano")
                            }
                        }

                        GoalProgressRow(
                            label = "Meta Faturamento Mensal",
                            currentKz = metrics.monthlyRevenueKz,
                            targetKz = metrics.monthlyRevenueGoalKz,
                            barColor = rumoColors.profit
                        )
                        GoalProgressRow(
                            label = "Teto de Despesas (Mês)",
                            currentKz = metrics.totalMonthlyExpensesKz,
                            targetKz = metrics.monthlyExpenseBudgetKz,
                            barColor = if (metrics.totalMonthlyExpensesKz > metrics.monthlyExpenseBudgetKz && metrics.monthlyExpenseBudgetKz > 0) {
                                rumoColors.loss
                            } else rumoColors.warning
                        )
                        GoalProgressRow(
                            label = "Reserva de Emergência",
                            currentKz = metrics.emergencyReserveKz,
                            targetKz = metrics.emergencyReserveTargetKz,
                            barColor = MaterialTheme.colorScheme.primary
                        )
                        GoalProgressRow(
                            label = "Meta Faturamento Anual",
                            currentKz = metrics.annualRevenueKz,
                            targetKz = metrics.annualRevenueGoalKz,
                            barColor = rumoColors.heroGold
                        )
                    }
                }
            }

            // 6. Top Monthly Categories Breakdown
            if (metrics.topCategories.isNotEmpty()) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Principais Categorias (Mês)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            metrics.topCategories.forEachIndexed { idx, cat ->
                                CategoryBreakdownRow(
                                    item = cat,
                                    color = if (cat.isRevenue) rumoColors.profit else rumoColors.loss
                                )
                                if (idx < metrics.topCategories.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // 7. Monthly & Daily Cashflow Bar Chart (Native Compose Canvas)
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashflow_evolution_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Evolução do Fluxo de Caixa (Kz)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(color = rumoColors.profit, label = "Entradas")
                                LegendDot(color = rumoColors.loss, label = "Saídas")
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            CashflowChartMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = selectedChartMode == mode,
                                    onClick = { selectedChartMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = CashflowChartMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, maxLines = 1)
                                }
                            }
                        }

                        val activePoints = if (selectedChartMode == CashflowChartMode.MONTHLY_6M) {
                            metrics.monthlyCashflowBars
                        } else {
                            metrics.dailyCashflowBars
                        }

                        CashflowBarChart(
                            points = activePoints,
                            emptyHint = if (selectedChartMode == CashflowChartMode.MONTHLY_6M) {
                                "Regista movimentos liquidados para visualizar a evolução mensal em Kz."
                            } else {
                                "Regista movimentos liquidados para visualizar o fluxo dos últimos 7 dias."
                            },
                            inflowColor = rumoColors.profit,
                            outflowColor = rumoColors.loss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryMetricCard(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdown,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isRevenue) "Entrada • ${Formatters.formatPercent(item.sharePercent)}" else "Saída • ${Formatters.formatPercent(item.sharePercent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = Formatters.formatKz(item.totalKz, compact = true),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoalProgressRow(
    label: String,
    currentKz: Double,
    targetKz: Double,
    barColor: Color
) {
    val ratio = if (targetKz > 0.0) (currentKz / targetKz).toFloat().coerceIn(0f, 1f) else 0f
    val pct = if (targetKz > 0.0) (currentKz / targetKz) * 100.0 else 0.0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (targetKz > 0) {
                    "${Formatters.formatKz(currentKz, compact = true)} / ${Formatters.formatKz(targetKz, compact = true)} (${Formatters.formatPercent(pct)})"
                } else {
                    "${Formatters.formatKz(currentKz, compact = true)} (Sem meta)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CashflowBarChart(
    points: List<DailyCashflowPoint>,
    emptyHint: String,
    inflowColor: Color,
    outflowColor: Color
) {
    if (points.isEmpty() || points.all { it.inflowKz == 0.0 && it.outflowKz == 0.0 }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]
    val maxVal = points.maxOf { maxOf(it.inflowKz, it.outflowKz) }.coerceAtLeast(1.0)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Y-Axis Kz reference scale
            Column(
                modifier = Modifier
                    .height(144.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatAxisKz(maxVal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAxisKz(maxVal / 2.0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val groupWidth = size.width.toFloat() / points.size
                            val tapped = (offset.x / groupWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = tapped
                        }
                    }
            ) {
                val groupWidth = size.width / points.size
                val barWidth = (groupWidth * 0.28f).coerceIn(8f, 34f)
                val gap = 4f
                val baseLine = size.height - 4f
                val chartHeight = baseLine - 8f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                // Horizontal reference gridlines
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = baseLine - (chartHeight * fraction)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f,
                        pathEffect = if (fraction > 0f) dashEffect else null
                    )
                }

                // Active period highlight
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(safeIndex * groupWidth + 4f, 0f),
                    size = Size((groupWidth - 8f).coerceAtLeast(1f), baseLine),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                points.forEachIndexed { index, point ->
                    val centerX = index * groupWidth + groupWidth / 2f
                    val inHeight = ((point.inflowKz / maxVal) * chartHeight).toFloat()
                    val outHeight = ((point.outflowKz / maxVal) * chartHeight).toFloat()

                    if (inHeight > 0f) {
                        drawRoundRect(
                            color = inflowColor,
                            topLeft = Offset(centerX - barWidth - gap / 2f, baseLine - inHeight),
                            size = Size(barWidth, inHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    if (outHeight > 0f) {
                        drawRoundRect(
                            color = outflowColor,
                            topLeft = Offset(centerX + gap / 2f, baseLine - outHeight),
                            size = Size(barWidth, outHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }

        // Interactive X-axis labels aligned with bar columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 34.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = index == safeIndex
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Selected Period Detail Strip in Kz
        val netBalance = selectedPoint.inflowKz - selectedPoint.outflowKz
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPoint.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+${Formatters.formatKz(selectedPoint.inflowKz, compact = true)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = inflowColor
                    )
                    Text(
                        text = "-${Formatters.formatKz(selectedPoint.outflowKz, compact = true)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = outflowColor
                    )
                    Text(
                        text = "Saldo: ${Formatters.formatKz(netBalance, compact = true)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (netBalance >= 0) inflowColor else outflowColor
                    )
                }
            }
        }
    }
}

private fun formatAxisKz(value: Double): String {
    return when {
        value >= 1_000_000.0 -> "${"%.1f".format(value / 1_000_000.0)}M"
        value >= 1_000.0 -> "${"%.0f".format(value / 1_000.0)}k"
        else -> "${"%.0f".format(value)}"
    }
}

