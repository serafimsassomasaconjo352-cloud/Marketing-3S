package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiTaskState
import com.example.model.CfoChatMessage
import com.example.model.PeriodReport
import com.example.model.ReportPeriodType
import com.example.model.UiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRumoColors

@Composable
fun CfoAdvisorScreen(
    chatMessages: List<CfoChatMessage>,
    chatBusy: Boolean,
    reportsState: UiState<List<PeriodReport>>,
    reportGenState: AiTaskState,
    onSendChatMessage: (String) -> Unit,
    onGenerateReport: (ReportPeriodType) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearReportGenState: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                    text = "CFO IA & Relatórios",
                    style = MaterialTheme.typography.headlineMedium
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("advisor_tab_chat")
                    ) {
                        Text("Consultor CFO IA")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("advisor_tab_reports")
                    ) {
                        Text("Relatórios de Período")
                    }
                }
            }

            if (selectedTab == 0) {
                CfoChatPane(
                    messages = chatMessages,
                    chatBusy = chatBusy,
                    onSend = onSendChatMessage,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PeriodReportsPane(
                    reportsState = reportsState,
                    reportGenState = reportGenState,
                    onGenerateReport = onGenerateReport,
                    onDeleteReport = onDeleteReport,
                    onClearReportGenState = onClearReportGenState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CfoChatPane(
    messages: List<CfoChatMessage>,
    chatBusy: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = remember {
        listOf(
            "Analisa o nosso fluxo de caixa e liquidez atual em Kz",
            "Identifica desperdícios e custos cortáveis",
            "Aplica a regra 80/20 para aumentar o faturamento da RUMO 3S IA",
            "Como aumentar receitas recorrentes em Angola e proteger margem contra câmbio?",
            "Avalia a nossa reserva de emergência e dívidas"
        )
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { if (!chatBusy) onSend(prompt) },
                    label = { Text(prompt, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        tonalElevation = if (isUser) 0.dp else 2.dp,
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (msg.text.isBlank() && msg.isStreaming) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("CFO IA a analisar os teus números...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Pergunta ao CFO da RUMO 3S IA...") },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cfo_chat_input")
                )
                IconButton(
                    enabled = inputText.isNotBlank() && !chatBusy,
                    onClick = {
                        val toSend = inputText
                        inputText = ""
                        onSend(toSend)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (inputText.isNotBlank() && !chatBusy) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("cfo_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar pergunta ao CFO IA",
                        tint = if (inputText.isNotBlank() && !chatBusy) {
                            MaterialTheme.colorScheme.onPrimary
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodReportsPane(
    reportsState: UiState<List<PeriodReport>>,
    reportGenState: AiTaskState,
    onGenerateReport: (ReportPeriodType) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearReportGenState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriodType.MONTHLY) }
    val rumoColors = LocalRumoColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Fecho & Relatório Executivo", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriodType.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.labelPt) }
                        )
                    }
                }
                Button(
                    enabled = reportGenState !is AiTaskState.Loading,
                    onClick = {
                        onClearReportGenState()
                        onGenerateReport(selectedPeriod)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_report_button")
                ) {
                    if (reportGenState is AiTaskState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("A gerar relatório ${selectedPeriod.labelPt}...")
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar Relatório ${selectedPeriod.labelPt} com IA")
                    }
                }

                when (reportGenState) {
                    is AiTaskState.Success -> {
                        Text(
                            text = reportGenState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = rumoColors.profit
                        )
                    }
                    is AiTaskState.Error -> {
                        Text(
                            text = reportGenState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> Unit
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (reportsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(reportsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val reports = reportsState.data
                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ainda não existem relatórios guardados. Seleciona o período acima para gerar o fecho executivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportCard(
                                report = report,
                                profitColor = rumoColors.profit,
                                lossColor = rumoColors.loss,
                                onDelete = { onDeleteReport(report.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: PeriodReport,
    profitColor: androidx.compose.ui.graphics.Color,
    lossColor: androidx.compose.ui.graphics.Color,
    onDelete: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${report.resolvedPeriodType.labelPt} • ${report.periodLabel}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    report.createdAt?.toDate()?.let { date ->
                        Text(
                            text = "Gerado em ${Formatters.formatDate(date.time)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remover relatório",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportNumberTile("Receitas", Formatters.formatKz(report.totalRevenueKz, compact = true), profitColor, Modifier.weight(1f))
                ReportNumberTile("Despesas", Formatters.formatKz(report.totalExpenseKz, compact = true), lossColor, Modifier.weight(1f))
                ReportNumberTile(
                    "Resultado",
                    Formatters.formatKz(report.netProfitKz, compact = true),
                    if (report.netProfitKz >= 0) profitColor else lossColor,
                    Modifier.weight(1f)
                )
            }

            Text("Resumo Financeiro", style = MaterialTheme.typography.labelLarge)
            Text(report.summary, style = MaterialTheme.typography.bodySmall)

            BulletSection("Problemas Identificados", report.problemsIdentified)
            BulletSection("Oportunidades de Melhoria", report.opportunities)
            BulletSection("Plano de Ação Próximo Período", report.actionPlan)
        }
    }
}

@Composable
private fun ReportNumberTile(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        items.forEach { item ->
            Text("• $item", style = MaterialTheme.typography.bodySmall)
        }
    }
}
