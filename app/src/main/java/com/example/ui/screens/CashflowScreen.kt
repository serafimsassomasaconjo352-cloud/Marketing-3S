package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FinancialTransaction
import com.example.model.TransactionType
import com.example.model.UiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRumoColors

private enum class CashflowFilter(val label: String) {
    ALL("Todos"),
    REVENUES("Receitas"),
    EXPENSES("Saídas"),
    DEBTS("Dívidas Pendentes"),
    RECURRING("Recorrentes"),
    ALERTS("Desperdício/Pessoal")
}

@Composable
fun CashflowScreen(
    transactionsState: UiState<List<FinancialTransaction>>,
    isSaving: Boolean,
    onAddTransaction: (FinancialTransaction, () -> Unit) -> Unit,
    onToggleSettled: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(CashflowFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    val rumoColors = LocalRumoColors.current

    if (showAddDialog) {
        AddTransactionDialog(
            isSaving = isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { tx ->
                onAddTransaction(tx) { showAddDialog = false }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registar entrada ou saída")
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
                // Header & Filter Chips
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Controlo de Caixa & Dívidas",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CashflowFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("cash_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                when (transactionsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = transactionsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is UiState.Success -> {
                        val allTxs = transactionsState.data
                        val filtered = remember(allTxs, selectedFilter) {
                            when (selectedFilter) {
                                CashflowFilter.ALL -> allTxs
                                CashflowFilter.REVENUES -> allTxs.filter { it.resolvedType == TransactionType.REVENUE }
                                CashflowFilter.EXPENSES -> allTxs.filter { it.resolvedType != TransactionType.REVENUE }
                                CashflowFilter.DEBTS -> allTxs.filter { !it.settled }
                                CashflowFilter.RECURRING -> allTxs.filter { it.recurring }
                                CashflowFilter.ALERTS -> allTxs.filter {
                                    it.wastefulFlag || !it.personalSeparated || it.resolvedType == TransactionType.PERSONAL_WITHDRAWAL
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Sem movimentos neste filtro",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Toca no botão + para registar entradas, custos, dívidas ou retiradas em Kwanzas (Kz).",
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { tx ->
                                    TransactionRowCard(
                                        tx = tx,
                                        profitColor = rumoColors.profit,
                                        lossColor = rumoColors.loss,
                                        warningColor = rumoColors.warning,
                                        onToggleSettled = { onToggleSettled(tx) },
                                        onDelete = { onDeleteTransaction(tx.id) }
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
private fun TransactionRowCard(
    tx: FinancialTransaction,
    profitColor: androidx.compose.ui.graphics.Color,
    lossColor: androidx.compose.ui.graphics.Color,
    warningColor: androidx.compose.ui.graphics.Color,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit
) {
    val type = tx.resolvedType
    val isInflow = type.isInflow
    val tint = if (isInflow) profitColor else lossColor

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isInflow) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = type.labelPt,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.category} • ${type.labelPt} • ${Formatters.formatDate(tx.transactionDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (if (isInflow) "+" else "-") + Formatters.formatKz(tx.amountKz, compact = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tint
                )
            }

            if (!tx.notes.isNullOrBlank()) {
                Text(
                    text = tx.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Badges & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!tx.settled) {
                        StatusPill(label = "Pendente / Dívida", color = warningColor, icon = Icons.Default.Schedule)
                    } else {
                        StatusPill(label = "Liquidado", color = profitColor, icon = Icons.Default.CheckCircle)
                    }
                    if (tx.recurring) {
                        StatusPill(label = "Recorrente", color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Repeat)
                    }
                    if (tx.wastefulFlag || !tx.personalSeparated || type == TransactionType.PERSONAL_WITHDRAWAL) {
                        StatusPill(
                            label = if (tx.wastefulFlag) "Desperdício" else "Conta Pessoal",
                            color = lossColor,
                            icon = Icons.Default.Warning
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onToggleSettled) {
                        Text(if (tx.settled) "Marcar Pendente" else "Liquidar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar movimento",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (FinancialTransaction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.REVENUE) }
    var category by remember { mutableStateOf("Serviços IA") }
    var notes by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(true) }
    var wastefulFlag by remember { mutableStateOf(false) }
    var personalSeparated by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val quickCategories = remember {
        listOf(
            "Serviços IA", "Consultoria", "Licenças SaaS", "Salários & IRT",
            "Energia/Internet", "Impostos AGT", "Marketing", "Infraestrutura", "Pró-labore"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Movimento (Kz)", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Classificação Financeira", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == TransactionType.PERSONAL_WITHDRAWAL) {
                                    personalSeparated = false
                                    category = "Pró-labore"
                                } else {
                                    personalSeparated = true
                                }
                            },
                            label = { Text(type.labelPt) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor em Kwanzas (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )

                Text("Categoria", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria personalizada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Justificação / Observações") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                SwitchRow(
                    label = "Já Pago / Recebido (desliga p/ Dívida)",
                    checked = settled,
                    onCheckedChange = { settled = it }
                )
                SwitchRow(
                    label = "Recorrente (mensal / contrato)",
                    checked = recurring,
                    onCheckedChange = { recurring = it }
                )
                if (!selectedType.isInflow) {
                    SwitchRow(
                        label = "Sinalizar possível desperdício",
                        checked = wastefulFlag,
                        onCheckedChange = { wastefulFlag = it }
                    )
                }
                SwitchRow(
                    label = "Conta 100% empresarial (separada da pessoal)",
                    checked = personalSeparated,
                    onCheckedChange = { personalSeparated = it }
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                modifier = Modifier.testTag("tx_save_button"),
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanCat = category.trim()
                    val amount = Formatters.parsePositiveKz(amountText)
                    if (cleanTitle.isEmpty()) {
                        errorMsg = "Insere uma descrição para o movimento."
                        return@Button
                    }
                    if (amount == null) {
                        errorMsg = "Insere um valor válido em Kwanzas maior que zero."
                        return@Button
                    }
                    if (cleanCat.isEmpty()) {
                        errorMsg = "Seleciona ou escreve uma categoria."
                        return@Button
                    }
                    errorMsg = null
                    onSave(
                        FinancialTransaction(
                            title = cleanTitle,
                            amountKz = amount,
                            type = selectedType.name,
                            category = cleanCat,
                            notes = notes.trim().ifBlank { null },
                            transactionDateMillis = System.currentTimeMillis(),
                            recurring = recurring,
                            settled = settled,
                            wastefulFlag = wastefulFlag,
                            personalSeparated = personalSeparated
                        )
                    )
                }
            ) {
                Text(if (isSaving) "A guardar..." else "Registar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
