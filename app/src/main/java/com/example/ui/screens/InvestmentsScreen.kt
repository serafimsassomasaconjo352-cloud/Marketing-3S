package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AiTaskState
import com.example.model.DashboardMetrics
import com.example.model.InvestmentFocus
import com.example.model.InvestmentItem
import com.example.model.InvestmentStatus
import com.example.model.RiskLevel
import com.example.model.UiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRumoColors

@Composable
fun InvestmentsScreen(
    metrics: DashboardMetrics,
    investmentsState: UiState<List<InvestmentItem>>,
    reviewState: AiTaskState,
    isSaving: Boolean,
    onAddInvestment: (InvestmentItem, () -> Unit) -> Unit,
    onUpdateInvestmentStatus: (InvestmentItem, InvestmentStatus, Double) -> Unit,
    onDeleteInvestment: (String) -> Unit,
    onEvaluateWithAi: (InvestmentItem) -> Unit,
    onClearReview: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InvestmentItem?>(null) }
    val rumoColors = LocalRumoColors.current

    if (showAddDialog) {
        AddInvestmentDialog(
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                onAddInvestment(item) { showAddDialog = false }
            }
        )
    }

    editingItem?.let { item ->
        UpdateInvestmentDialog(
            item = item,
            onDismiss = { editingItem = null },
            onConfirm = { newStatus, actualReturn ->
                onUpdateInvestmentStatus(item, newStatus, actualReturn)
                editingItem = null
            }
        )
    }

    if (reviewState !is AiTaskState.Idle) {
        AlertDialog(
            onDismissRequest = onClearReview,
            title = { Text("Parecer Estratégico CFO IA") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (reviewState) {
                        is AiTaskState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("A analisar risco, ROI e liquidez em Kz...")
                            }
                        }
                        is AiTaskState.Success -> {
                            Text(reviewState.message, style = MaterialTheme.typography.bodyMedium)
                        }
                        is AiTaskState.Error -> {
                            Text(
                                reviewState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        AiTaskState.Idle -> Unit
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearReview) { Text("Fechar") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_investment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova oportunidade de investimento")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Investimentos & Regra 80/20",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ROI Médio Ativo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = Formatters.formatPercent(metrics.averageRoiPercent),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Receita Recorrente/Mês",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = Formatters.formatKz(metrics.recurringRevenueKz, compact = true),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                when (investmentsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(investmentsState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is UiState.Success -> {
                        val items = investmentsState.data
                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Sem projetos de investimento registados",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Adiciona oportunidades de crescimento, automação ou receitas recorrentes para calcular ROI, risco e validar com o CFO IA.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 88.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items, key = { it.id }) { item ->
                                    InvestmentCard(
                                        item = item,
                                        profitColor = rumoColors.profit,
                                        lossColor = rumoColors.loss,
                                        goldColor = rumoColors.heroGold,
                                        onEditStatus = { editingItem = item },
                                        onEvaluateAi = { onEvaluateWithAi(item) },
                                        onDelete = { onDeleteInvestment(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(
    item: InvestmentItem,
    profitColor: androidx.compose.ui.graphics.Color,
    lossColor: androidx.compose.ui.graphics.Color,
    goldColor: androidx.compose.ui.graphics.Color,
    onEditStatus: () -> Unit,
    onEvaluateAi: () -> Unit,
    onDelete: () -> Unit
) {
    val roi = if (item.actualReturnKz > 0) item.realizedRoiPercent else item.projectedRoiPercent
    val roiColor = if (roi >= 0) profitColor else lossColor

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.pareto8020) {
                            Surface(color = goldColor.copy(alpha = 0.2f), shape = CircleShape) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Alavanca 80/20",
                                        tint = goldColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "80/20",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = goldColor
                                    )
                                }
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Text(
                                text = item.resolvedStatus.labelPt,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${item.resolvedFocus.labelPt} • ${item.resolvedRisk.labelPt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ROI ${Formatters.formatPercent(roi)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = roiColor
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remover investimento",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Capital", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.formatKz(item.capitalKz, compact = true),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Retorno Prev.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.formatKz(item.expectedReturnKz, compact = true),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Retorno Real", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.formatKz(item.actualReturnKz, compact = true),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
            }

            if (item.justification.isNotBlank()) {
                Text(
                    text = item.justification,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditStatus,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Atualizar Estado")
                }
                Button(
                    onClick = onEvaluateAi,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Avaliar CFO IA")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddInvestmentDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (InvestmentItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var capitalText by remember { mutableStateOf("") }
    var expectedReturnText by remember { mutableStateOf("") }
    var risk by remember { mutableStateOf(RiskLevel.LOW) }
    var focus by remember { mutableStateOf(InvestmentFocus.RECURRING_REVENUE) }
    var pareto8020 by remember { mutableStateOf(true) }
    var justification by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avaliar Investimento (Kz)", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Iniciativa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("inv_title_input")
                )
                OutlinedTextField(
                    value = capitalText,
                    onValueChange = { capitalText = it },
                    label = { Text("Capital Necessário (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("inv_capital_input")
                )
                OutlinedTextField(
                    value = expectedReturnText,
                    onValueChange = { expectedReturnText = it },
                    label = { Text("Retorno Esperado Bruto (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("inv_return_input")
                )

                Text("Nível de Risco", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RiskLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = risk == lvl,
                            onClick = { risk = lvl },
                            label = { Text(lvl.labelPt) }
                        )
                    }
                }

                Text("Eixo de Crescimento", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InvestmentFocus.entries.forEach { fc ->
                        FilterChip(
                            selected = focus == fc,
                            onClick = { focus = fc },
                            label = { Text(fc.labelPt) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Prioridade 80/20 (20% esforço -> 80% resultado)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = pareto8020, onCheckedChange = { pareto8020 = it })
                }

                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it },
                    label = { Text("Justificação Estratégica") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                modifier = Modifier.testTag("inv_save_button"),
                onClick = {
                    val cleanTitle = title.trim()
                    val cap = Formatters.parsePositiveKz(capitalText)
                    val ret = Formatters.parseNonNegativeKz(expectedReturnText)
                    if (cleanTitle.isEmpty()) {
                        errorMsg = "Insere o nome do investimento."
                        return@Button
                    }
                    if (cap == null || ret == null) {
                        errorMsg = "Insere valores válidos em Kwanzas."
                        return@Button
                    }
                    errorMsg = null
                    onSave(
                        InvestmentItem(
                            title = cleanTitle,
                            capitalKz = cap,
                            expectedReturnKz = ret,
                            actualReturnKz = 0.0,
                            riskLevel = risk.name,
                            focusType = focus.name,
                            status = InvestmentStatus.PROPOSED.name,
                            pareto8020 = pareto8020,
                            justification = justification.trim()
                        )
                    )
                }
            ) {
                Text(if (isSaving) "A guardar..." else "Registar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateInvestmentDialog(
    item: InvestmentItem,
    onDismiss: () -> Unit,
    onConfirm: (InvestmentStatus, Double) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(item.resolvedStatus) }
    var actualReturnText by remember {
        mutableStateOf(item.actualReturnKz.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar ${item.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estado", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InvestmentStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.labelPt) }
                        )
                    }
                }
                OutlinedTextField(
                    value = actualReturnText,
                    onValueChange = { actualReturnText = it },
                    label = { Text("Retorno Realizado (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!err.isNullOrBlank()) {
                    Text(err ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val actual = Formatters.parseNonNegativeKz(actualReturnText)
                if (actual == null) {
                    err = "Valor inválido."
                    return@Button
                }
                onConfirm(selectedStatus, actual)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
