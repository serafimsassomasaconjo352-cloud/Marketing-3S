package com.example.data

import android.util.Log
import com.example.model.CfoChatMessage
import com.example.model.DashboardMetrics
import com.example.model.FinancialTransaction
import com.example.model.InvestmentItem
import com.example.model.PeriodReport
import com.example.model.ReportPeriodType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class GeneratedReportContent(
    val summary: String,
    val problemsIdentified: List<String>,
    val opportunities: List<String>,
    val actionPlan: List<String>
)

class CfoAiRepository {
    companion object {
        private const val TAG = "CfoAiRepository"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val TIMEOUT_MS = 60_000L
    }

    private val cfoSystemInstruction = content {
        text(
            """
            És o Diretor Financeiro (CFO), contabilista de gestão e consultor estratégico da empresa RUMO 3S IA, operando em Angola.
            Regras obrigatórias de atuação:
            1. NUNCA inventes números financeiros. Usa EXCLUSIVAMENTE os dados reais fornecidos no contexto financeiro. Se faltarem registos, declara claramente o que falta registar.
            2. Justifica todas as recomendações com base nos números reais e no princípio 80/20 (Pareto).
            3. Reforça sempre a separação rigorosa entre dinheiro pessoal dos sócios e caixa empresarial da RUMO 3S IA.
            4. Nunca confundas faturamento (receita bruta) com lucro líquido.
            5. Expressa todos os valores monetários em Kwanzas (Kz).
            6. Apresenta soluções práticas e realistas para a realidade económica de Angola (gestão cambial de ferramentas SaaS/IA em USD vs receitas em Kz, retenção na fonte, IVA/AGT, liquidez, contratos B2B recorrentes, eficiência operacional).
            7. Prioriza sustentabilidade financeira, reserva de emergência, transparência e crescimento estável a longo prazo.
            Responde de forma objetiva, estruturada e direta em Português.
            """.trimIndent()
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = cfoSystemInstruction
        )
    }

    private val reportSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Resumo executivo da saúde financeira no período em Kwanzas"),
            "problemsIdentified" to Schema.array(
                items = Schema.string(),
                description = "Lista de até 5 problemas ou riscos financeiros reais identificados"
            ),
            "opportunities" to Schema.array(
                items = Schema.string(),
                description = "Lista de até 5 oportunidades de melhoria de margem, 80/20 ou receita recorrente"
            ),
            "actionPlan" to Schema.array(
                items = Schema.string(),
                description = "Plano de ação concreto para o próximo período em Angola (até 5 passos)"
            )
        )
    )

    private val structuredReportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = cfoSystemInstruction,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
            }
        )
    }

    fun buildFinancialContextSnapshot(
        metrics: DashboardMetrics,
        recentTransactions: List<FinancialTransaction>,
        investments: List<InvestmentItem>
    ): String {
        val kzFmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("pt-AO")))
        fun fmt(v: Double) = "${kzFmt.format(v)} Kz"

        val txLines = recentTransactions.take(15).joinToString("\n") { tx ->
            "- ${tx.title} | ${tx.resolvedType.labelPt} | ${fmt(tx.amountKz)} | Cat: ${tx.category} | " +
                "Liquidado=${tx.settled} | Recorrente=${tx.recurring} | Desperdício=${tx.wastefulFlag} | SeparaçãoPessoal=${tx.personalSeparated}"
        }.ifBlank { "(Sem lançamentos registados)" }

        val invLines = investments.take(10).joinToString("\n") { inv ->
            "- ${inv.title} | Capital: ${fmt(inv.capitalKz)} | Retorno Esperado: ${fmt(inv.expectedReturnKz)} | " +
                "Retorno Real: ${fmt(inv.actualReturnKz)} | ROI Est: ${"%.1f".format(inv.projectedRoiPercent)}% | " +
                "Risco: ${inv.resolvedRisk.labelPt} | 80/20=${inv.pareto8020} | Estado: ${inv.resolvedStatus.labelPt}"
        }.ifBlank { "(Sem investimentos registados)" }

        return """
            === DADOS FINANCEIROS REAIS DA RUMO 3S IA ===
            Plano Configurado: ${metrics.hasPlanConfigured}
            Capital Disponível: ${fmt(metrics.availableCapitalKz)}
            Faturamento Diário: ${fmt(metrics.dailyRevenueKz)}
            Faturamento Semanal: ${fmt(metrics.weeklyRevenueKz)}
            Faturamento Mensal: ${fmt(metrics.monthlyRevenueKz)} (Meta: ${fmt(metrics.monthlyRevenueGoalKz)})
            Faturamento Anual: ${fmt(metrics.annualRevenueKz)} (Meta: ${fmt(metrics.annualRevenueGoalKz)})
            Receita Recorrente (mês): ${fmt(metrics.recurringRevenueKz)}
            Custos Diretos (mês): ${fmt(metrics.monthlyDirectCostsKz)}
            Custos Operacionais (mês): ${fmt(metrics.monthlyOperationalCostsKz)} (Orçamento Máx: ${fmt(metrics.monthlyExpenseBudgetKz)})
            Lucro Bruto (mês): ${fmt(metrics.grossProfitKz)}
            Lucro Líquido (mês): ${fmt(metrics.netProfitKz)}
            Margem de Lucro Líquida: ${"%.1f".format(metrics.profitMarginPercent)}%
            Dívidas Pendentes: ${fmt(metrics.activeDebtsKz)}
            Reserva de Emergência: ${fmt(metrics.emergencyReserveKz)} (Meta: ${fmt(metrics.emergencyReserveTargetKz)}, Cobertura: ${"%.1f".format(metrics.runwayMonths)} meses)
            Necessidade Prevista de Capital: ${fmt(metrics.capitalNeedsForecastKz)}
            ROI Médio de Investimentos: ${"%.1f".format(metrics.averageRoiPercent)}%
            Desperdícios Sinalizados: ${fmt(metrics.wastefulSpendingKz)}
            Retiradas Pessoais / Pró-labore: ${fmt(metrics.personalWithdrawalsKz)} (Alertas mistura pessoal: ${metrics.personalMixAlertsCount})

            Movimentos Recentes:
            $txLines

            Carteira de Investimentos & Iniciativas:
            $invLines
        """.trimIndent()
    }

    fun streamCfoConsultation(
        history: List<CfoChatMessage>,
        question: String,
        financialContextSnapshot: String
    ): Flow<String> = flow {
        val chatHistory = history
            .filter { !it.isStreaming && it.text.isNotBlank() }
            .takeLast(8)
            .map { msg ->
                content(if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

        val chat = conversationalModel.startChat(history = chatHistory)
        val promptWithContext = """
            $financialContextSnapshot

            Pedido do Gestor / Sócio da RUMO 3S IA:
            $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            chat.sendMessageStream(promptWithContext).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("O Consultor CFO IA não devolveu resposta.")
        }
    }

    suspend fun evaluateInvestment(
        investment: InvestmentItem,
        financialContextSnapshot: String
    ): String {
        val prompt = """
            $financialContextSnapshot

            Avalia especificamente esta oportunidade de investimento para a RUMO 3S IA em Angola:
            - Iniciativa: ${investment.title}
            - Capital Necessário: ${investment.capitalKz} Kz
            - Retorno Esperado: ${investment.expectedReturnKz} Kz (ROI estimado: ${"%.1f".format(investment.projectedRoiPercent)}%)
            - Risco: ${investment.resolvedRisk.labelPt}
            - Foco: ${investment.resolvedFocus.labelPt}
            - Marcado como 80/20: ${investment.pareto8020}
            - Justificação do sócio: ${investment.justification}

            Dá um parecer executivo de CFO:
            1) É sustentável face ao capital disponível e à reserva de emergência atual?
            2) Enquadra-se no princípio 80/20 e gera receita recorrente ou melhoria de margem?
            3) Riscos cambiais/operacionais em Angola e veredito final (Aprovar, Adiar ou Rejeitar com condições).
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            conversationalModel.generateContent(prompt)
        }
        val text = response.text
        if (text.isNullOrBlank()) {
            throw IllegalStateException("Resposta vazia ao avaliar investimento.")
        }
        return text
    }

    suspend fun generateStructuredPeriodReport(
        periodType: ReportPeriodType,
        periodLabel: String,
        totalRevenueKz: Double,
        totalExpenseKz: Double,
        netProfitKz: Double,
        financialContextSnapshot: String
    ): GeneratedReportContent {
        val prompt = """
            $financialContextSnapshot

            Gera o Relatório Financeiro Executivo de encerramento do período ${periodType.labelPt} ($periodLabel) para a RUMO 3S IA.
            Valores exatos calculados no período:
            - Receitas do Período: $totalRevenueKz Kz
            - Despesas e Custos do Período: $totalExpenseKz Kz
            - Resultado (Lucro/Prejuízo): $netProfitKz Kz

            Não inventes números fora dos fornecidos. Identifica problemas reais (desperdícios, dívidas, falta de reserva, mistura pessoal, ou ausência de lançamentos), oportunidades 80/20 e de receitas recorrentes, e um plano de ação prático para o período seguinte em Angola.
        """.trimIndent()

        val response = withTimeout(TIMEOUT_MS) {
            structuredReportModel.generateContent(prompt)
        }
        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("O modelo devolveu um relatório vazio.")
        }
        return try {
            val json = JSONObject(jsonText)
            val summary = json.optString("summary", "Sem resumo.")
            val problemsArr = json.optJSONArray("problemsIdentified")
            val oppsArr = json.optJSONArray("opportunities")
            val planArr = json.optJSONArray("actionPlan")

            fun toList(arr: org.json.JSONArray?): List<String> =
                if (arr == null) emptyList()
                else (0 until arr.length()).mapNotNull { idx ->
                    arr.optString(idx).takeIf { it.isNotBlank() }
                }.take(10)

            GeneratedReportContent(
                summary = summary,
                problemsIdentified = toList(problemsArr),
                opportunities = toList(oppsArr),
                actionPlan = toList(planArr)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse structured report JSON", e)
            throw IllegalStateException("Erro ao interpretar o relatório financeiro estruturado.", e)
        }
    }
}
