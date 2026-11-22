package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CfoAiRepository
import com.example.data.FinancialCalculator
import com.example.data.FinancialRepository
import com.example.model.AiTaskState
import com.example.model.CfoChatMessage
import com.example.model.DashboardMetrics
import com.example.model.FinancialPlan
import com.example.model.FinancialTransaction
import com.example.model.InvestmentItem
import com.example.model.InvestmentStatus
import com.example.model.PeriodReport
import com.example.model.ReportPeriodType
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CfoViewModel(
    private val repository: FinancialRepository,
    private val aiRepository: CfoAiRepository,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "CfoViewModel"
        private const val STOP_TIMEOUT_MS = 5_000L
    }

    val planState: StateFlow<UiState<FinancialPlan?>> = repository.observeFinancialPlan(currentUserId)
        .toUiStateFlow("Falha ao carregar plano financeiro")

    val transactionsState: StateFlow<UiState<List<FinancialTransaction>>> =
        repository.observeTransactions(currentUserId)
            .toUiStateFlow("Falha ao carregar movimentos de caixa")

    val investmentsState: StateFlow<UiState<List<InvestmentItem>>> =
        repository.observeInvestments(currentUserId)
            .toUiStateFlow("Falha ao carregar carteira de investimentos")

    val reportsState: StateFlow<UiState<List<PeriodReport>>> =
        repository.observeReports(currentUserId)
            .toUiStateFlow("Falha ao carregar relatórios financeiros")

    val metricsState: StateFlow<DashboardMetrics> = combine(
        planState,
        transactionsState,
        investmentsState
    ) { planUi, txUi, invUi ->
        val plan = (planUi as? UiState.Success)?.data
        val txs = (txUi as? UiState.Success)?.data.orEmpty()
        val invs = (invUi as? UiState.Success)?.data.orEmpty()
        FinancialCalculator.computeMetrics(plan, txs, invs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DashboardMetrics()
    )

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            CfoChatMessage(
                id = "welcome",
                isUser = false,
                text = "Sou o Diretor Financeiro (CFO) e estrategista da RUMO 3S IA. " +
                    "Todas as minhas análises usam apenas os números reais que registares em Kwanzas (Kz). " +
                    "Podes pedir diagnóstico de fluxo de caixa, corte de desperdícios, estratégia 80/20 ou crescimento sustentável em Angola."
            )
        )
    )
    val chatMessages: StateFlow<List<CfoChatMessage>> = _chatMessages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

    private val _reportGenState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val reportGenState: StateFlow<AiTaskState> = _reportGenState.asStateFlow()

    private val _investmentReviewState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val investmentReviewState: StateFlow<AiTaskState> = _investmentReviewState.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun clearInvestmentReview() {
        _investmentReviewState.value = AiTaskState.Idle
    }

    fun clearReportGenState() {
        _reportGenState.value = AiTaskState.Idle
    }

    fun savePlan(plan: FinancialPlan, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveFinancialPlan(plan)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Plano financeiro e metas atualizados em Kz."
                    onDone()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Erro ao guardar o plano."
                }
            )
        }
    }

    fun addTransaction(tx: FinancialTransaction, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addTransaction(tx)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = if (!tx.personalSeparated || tx.type == "PERSONAL_WITHDRAWAL") {
                        "Movimento registado. Atenção: mantenha a conta pessoal separada do caixa da RUMO 3S IA."
                    } else {
                        "Movimento registado no livro-caixa em Kz."
                    }
                    onDone()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Erro ao registar movimento."
                }
            )
        }
    }

    fun toggleTransactionSettled(tx: FinancialTransaction) {
        if (tx.id.isBlank()) return
        viewModelScope.launch {
            val newSettled = !tx.settled
            val result = repository.setTransactionSettled(tx.id, newSettled)
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = if (newSettled) "Marcado como liquidado." else "Marcado como pendente/dívida."
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Erro ao atualizar estado."
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        if (transactionId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Movimento eliminado." },
                onFailure = { err -> _feedbackMessage.value = err.localizedMessage ?: "Erro ao eliminar." }
            )
        }
    }

    fun addInvestment(item: InvestmentItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addInvestment(item)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Iniciativa estratégica registada."
                    onDone()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Erro ao registar investimento."
                }
            )
        }
    }

    fun updateInvestmentStatus(item: InvestmentItem, newStatus: InvestmentStatus, actualReturnKz: Double) {
        if (item.id.isBlank()) return
        viewModelScope.launch {
            val result = repository.updateInvestment(item.id, newStatus, actualReturnKz)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Investimento atualizado para ${newStatus.labelPt}." },
                onFailure = { err -> _feedbackMessage.value = err.localizedMessage ?: "Erro ao atualizar investimento." }
            )
        }
    }

    fun deleteInvestment(investmentId: String) {
        if (investmentId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteInvestment(investmentId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Investimento removido." },
                onFailure = { err -> _feedbackMessage.value = err.localizedMessage ?: "Erro ao remover." }
            )
        }
    }

    fun deleteReport(reportId: String) {
        if (reportId.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteReport(reportId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Relatório removido." },
                onFailure = { err -> _feedbackMessage.value = err.localizedMessage ?: "Erro ao remover relatório." }
            )
        }
    }

    fun sendCfoChatMessage(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank() || _chatBusy.value) return

        val userMsg = CfoChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQuestion
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val historyBefore = _chatMessages.value

        _chatMessages.update {
            it + userMsg + CfoChatMessage(
                id = assistantMsgId,
                isUser = false,
                text = "",
                isStreaming = true
            )
        }
        _chatBusy.value = true

        viewModelScope.launch {
            try {
                val contextSnapshot = buildSnapshot()
                aiRepository.streamCfoConsultation(historyBefore, cleanQuestion, contextSnapshot)
                    .collect { partialText ->
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = partialText, isStreaming = true)
                                } else msg
                            }
                        }
                    }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "CFO chat timed out", e)
                updateAssistantError(
                    assistantMsgId,
                    "Tempo limite excedido (60s). Verifica a ligação e tenta novamente."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "CFO chat failed", e)
                updateAssistantError(
                    assistantMsgId,
                    "Erro na análise CFO IA: ${e.localizedMessage ?: "Falha na comunicação."}"
                )
            } finally {
                _chatBusy.value = false
            }
        }
    }

    fun evaluateInvestmentWithAi(investment: InvestmentItem) {
        viewModelScope.launch {
            _investmentReviewState.value = AiTaskState.Loading
            try {
                val snapshot = buildSnapshot()
                val analysis = aiRepository.evaluateInvestment(investment, snapshot)
                _investmentReviewState.value = AiTaskState.Success(analysis)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Investment review timed out", e)
                _investmentReviewState.value = AiTaskState.Error("Análise expirou após 60s. Tenta novamente.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Investment review failed", e)
                _investmentReviewState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Falha ao consultar CFO IA."
                )
            }
        }
    }

    fun generateAndSavePeriodReport(periodType: ReportPeriodType) {
        viewModelScope.launch {
            _reportGenState.value = AiTaskState.Loading
            try {
                val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
                val (rev, exp, net) = FinancialCalculator.periodTotals(periodType, txs)
                val periodLabel = buildPeriodLabel(periodType)
                val snapshot = buildSnapshot()

                val generated = aiRepository.generateStructuredPeriodReport(
                    periodType = periodType,
                    periodLabel = periodLabel,
                    totalRevenueKz = rev,
                    totalExpenseKz = exp,
                    netProfitKz = net,
                    financialContextSnapshot = snapshot
                )

                val report = PeriodReport(
                    periodType = periodType.name,
                    periodLabel = periodLabel,
                    totalRevenueKz = rev,
                    totalExpenseKz = exp,
                    netProfitKz = net,
                    summary = generated.summary,
                    problemsIdentified = generated.problemsIdentified,
                    opportunities = generated.opportunities,
                    actionPlan = generated.actionPlan,
                    generatedByAi = true
                )

                val saveResult = repository.saveReport(report)
                saveResult.fold(
                    onSuccess = {
                        _reportGenState.value = AiTaskState.Success(
                            "Relatório ${periodType.labelPt} ($periodLabel) gerado e guardado com sucesso."
                        )
                    },
                    onFailure = { err ->
                        _reportGenState.value = AiTaskState.Error(
                            err.localizedMessage ?: "Relatório gerado, mas falhou ao guardar."
                        )
                    }
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Report generation timed out", e)
                _reportGenState.value = AiTaskState.Error("Geração do relatório expirou após 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Report generation failed", e)
                _reportGenState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Erro ao gerar relatório com IA."
                )
            }
        }
    }

    private fun updateAssistantError(messageId: String, errorText: String) {
        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(text = errorText, isStreaming = false)
                } else msg
            }
        }
    }

    private fun buildSnapshot(): String {
        val metrics = metricsState.value
        val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
        val invs = (investmentsState.value as? UiState.Success)?.data.orEmpty()
        return aiRepository.buildFinancialContextSnapshot(metrics, txs, invs)
    }

    private fun buildPeriodLabel(periodType: ReportPeriodType): String {
        val now = Date()
        val ptAo = Locale.forLanguageTag("pt-AO")
        return when (periodType) {
            ReportPeriodType.DAILY -> SimpleDateFormat("dd/MM/yyyy", ptAo).format(now)
            ReportPeriodType.WEEKLY -> "Semana ${SimpleDateFormat("w / yyyy", ptAo).format(now)}"
            ReportPeriodType.MONTHLY -> SimpleDateFormat("MMMM yyyy", ptAo).format(now)
                .replaceFirstChar { it.uppercase(ptAo) }
            ReportPeriodType.ANNUAL -> SimpleDateFormat("yyyy", ptAo).format(now)
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )
}
