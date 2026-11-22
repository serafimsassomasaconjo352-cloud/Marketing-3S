package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.FinancialPlan

@Composable
fun BudgetPlanDialog(
    currentPlan: FinancialPlan?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (FinancialPlan) -> Unit
) {
    var companyName by remember { mutableStateOf(currentPlan?.companyName ?: "RUMO 3S IA") }
    var openingCapitalText by remember {
        mutableStateOf(currentPlan?.openingCapitalKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var monthlyRevGoalText by remember {
        mutableStateOf(currentPlan?.monthlyRevenueGoalKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var monthlyBudgetText by remember {
        mutableStateOf(currentPlan?.monthlyExpenseBudgetKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var annualRevGoalText by remember {
        mutableStateOf(currentPlan?.annualRevenueGoalKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var reserveTargetText by remember {
        mutableStateOf(currentPlan?.emergencyReserveTargetKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var reserveBalanceText by remember {
        mutableStateOf(currentPlan?.emergencyReserveBalanceKz?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var notesText by remember { mutableStateOf(currentPlan?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Planeamento Financeiro (Kz)",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Define o orçamento mensal/anual, capital inicial e metas da RUMO 3S IA em Kwanzas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Empresa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_company_input")
                )
                OutlinedTextField(
                    value = openingCapitalText,
                    onValueChange = { openingCapitalText = it },
                    label = { Text("Capital Inicial Empresarial (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_opening_capital_input")
                )
                OutlinedTextField(
                    value = monthlyRevGoalText,
                    onValueChange = { monthlyRevGoalText = it },
                    label = { Text("Meta Faturamento Mensal (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_monthly_revenue_input")
                )
                OutlinedTextField(
                    value = monthlyBudgetText,
                    onValueChange = { monthlyBudgetText = it },
                    label = { Text("Teto Orçamento Despesas/Mês (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_monthly_budget_input")
                )
                OutlinedTextField(
                    value = annualRevGoalText,
                    onValueChange = { annualRevGoalText = it },
                    label = { Text("Meta Faturamento Anual (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_annual_revenue_input")
                )
                OutlinedTextField(
                    value = reserveBalanceText,
                    onValueChange = { reserveBalanceText = it },
                    label = { Text("Reserva de Emergência Inicial (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_reserve_balance_input")
                )
                OutlinedTextField(
                    value = reserveTargetText,
                    onValueChange = { reserveTargetText = it },
                    label = { Text("Meta Reserva de Emergência (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_reserve_target_input")
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Diretrizes / Notas do Orçamento") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                modifier = Modifier.testTag("plan_save_button"),
                onClick = {
                    val opening = Formatters.parseNonNegativeKz(openingCapitalText)
                    val mRev = Formatters.parseNonNegativeKz(monthlyRevGoalText)
                    val mExp = Formatters.parseNonNegativeKz(monthlyBudgetText)
                    val aRev = Formatters.parseNonNegativeKz(annualRevGoalText)
                    val rBal = Formatters.parseNonNegativeKz(reserveBalanceText)
                    val rTgt = Formatters.parseNonNegativeKz(reserveTargetText)

                    if (opening == null || mRev == null || mExp == null || aRev == null || rBal == null || rTgt == null) {
                        validationError = "Verifica os valores numéricos em Kwanzas (>= 0)."
                        return@Button
                    }
                    validationError = null
                    onSave(
                        FinancialPlan(
                            companyName = companyName.trim().ifBlank { "RUMO 3S IA" },
                            openingCapitalKz = opening,
                            monthlyRevenueGoalKz = mRev,
                            monthlyExpenseBudgetKz = mExp,
                            annualRevenueGoalKz = aRev,
                            emergencyReserveBalanceKz = rBal,
                            emergencyReserveTargetKz = rTgt,
                            notes = notesText.trim().ifBlank { null },
                            createdAt = currentPlan?.createdAt
                        )
                    )
                }
            ) {
                Text(if (isSaving) "A guardar..." else "Guardar Plano")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
