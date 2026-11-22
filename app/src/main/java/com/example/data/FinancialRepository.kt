package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.FinancialPlan
import com.example.model.FinancialTransaction
import com.example.model.InvestmentItem
import com.example.model.InvestmentStatus
import com.example.model.PeriodReport
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinancialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Utilizador deve autenticar-se com Google primeiro.")
    }

    fun observeFinancialPlan(userId: String): Flow<FinancialPlan?> = flow {
        val path = "users/$userId/settings/financial_plan"
        emitAll(
            db.collection("users").document(userId)
                .collection("settings").document("financial_plan")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FinancialPlan::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<FinancialTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId)
                .collection("transactions")
                .orderBy("transactionDateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FinancialTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeInvestments(userId: String): Flow<List<InvestmentItem>> = flow {
        val path = "users/$userId/investments"
        emitAll(
            db.collection("users").document(userId)
                .collection("investments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InvestmentItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeReports(userId: String): Flow<List<PeriodReport>> = flow {
        val path = "users/$userId/reports"
        emitAll(
            db.collection("users").document(userId)
                .collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PeriodReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveFinancialPlan(plan: FinancialPlan): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/settings/financial_plan"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("settings").document("financial_plan")
            docPath = docRef.path

            val company = plan.companyName.trim().ifBlank { "RUMO 3S IA" }.take(100)
            require(plan.monthlyRevenueGoalKz >= 0.0) { "Meta mensal deve ser >= 0" }
            require(plan.monthlyExpenseBudgetKz >= 0.0) { "Orçamento mensal deve ser >= 0" }
            require(plan.annualRevenueGoalKz >= 0.0) { "Meta anual deve ser >= 0" }
            require(plan.emergencyReserveTargetKz >= 0.0) { "Meta de reserva deve ser >= 0" }
            require(plan.emergencyReserveBalanceKz >= 0.0) { "Saldo de reserva deve ser >= 0" }
            require(plan.openingCapitalKz >= 0.0) { "Capital inicial deve ser >= 0" }

            val exists = docRef.get().await().exists()
            val cleanNotes = plan.notes?.trim()?.take(500)?.ifBlank { null }
            if (exists) {
                val updates = mutableMapOf<String, Any>(
                    "companyName" to company,
                    "monthlyRevenueGoalKz" to plan.monthlyRevenueGoalKz,
                    "monthlyExpenseBudgetKz" to plan.monthlyExpenseBudgetKz,
                    "annualRevenueGoalKz" to plan.annualRevenueGoalKz,
                    "emergencyReserveTargetKz" to plan.emergencyReserveTargetKz,
                    "emergencyReserveBalanceKz" to plan.emergencyReserveBalanceKz,
                    "openingCapitalKz" to plan.openingCapitalKz,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanNotes != null) updates["notes"] = cleanNotes
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "companyName" to company,
                    "monthlyRevenueGoalKz" to plan.monthlyRevenueGoalKz,
                    "monthlyExpenseBudgetKz" to plan.monthlyExpenseBudgetKz,
                    "annualRevenueGoalKz" to plan.annualRevenueGoalKz,
                    "emergencyReserveTargetKz" to plan.emergencyReserveTargetKz,
                    "emergencyReserveBalanceKz" to plan.emergencyReserveBalanceKz,
                    "openingCapitalKz" to plan.openingCapitalKz,
                    "notes" to cleanNotes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(tx: FinancialTransaction): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions"
        return try {
            val uid = requireUserId()
            val title = tx.title.trim().take(120)
            val category = tx.category.trim().take(60)
            require(title.isNotEmpty()) { "O título é obrigatório." }
            require(category.isNotEmpty()) { "A categoria é obrigatória." }
            require(tx.amountKz > 0.0 && tx.amountKz <= 1_000_000_000_000.0) {
                "O valor em Kwanzas deve ser positivo."
            }

            val docRef = db.collection("users").document(uid).collection("transactions").document()
            docPath = docRef.path
            val cleanNotes = tx.notes?.trim()?.take(400)?.ifBlank { null }
            val payload = mapOf(
                "title" to title,
                "amountKz" to tx.amountKz,
                "type" to tx.type,
                "category" to category,
                "notes" to cleanNotes,
                "transactionDateMillis" to tx.transactionDateMillis.coerceAtLeast(1L),
                "recurring" to tx.recurring,
                "settled" to tx.settled,
                "wastefulFlag" to tx.wastefulFlag,
                "personalSeparated" to tx.personalSeparated,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setTransactionSettled(transactionId: String, settled: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$transactionId"
        return try {
            val uid = requireUserId()
            require(transactionId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "settled" to settled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$transactionId"
        return try {
            val uid = requireUserId()
            require(transactionId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addInvestment(item: InvestmentItem): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/investments"
        return try {
            val uid = requireUserId()
            val title = item.title.trim().take(120)
            require(title.isNotEmpty()) { "Título do investimento obrigatório." }
            require(item.capitalKz > 0.0) { "Capital investido deve ser superior a zero." }

            val docRef = db.collection("users").document(uid).collection("investments").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to title,
                "capitalKz" to item.capitalKz,
                "expectedReturnKz" to item.expectedReturnKz.coerceAtLeast(0.0),
                "actualReturnKz" to item.actualReturnKz.coerceAtLeast(0.0),
                "riskLevel" to item.riskLevel,
                "focusType" to item.focusType,
                "status" to item.status,
                "pareto8020" to item.pareto8020,
                "justification" to item.justification.trim().take(600),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateInvestment(
        investmentId: String,
        status: InvestmentStatus,
        actualReturnKz: Double
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/investments/$investmentId"
        return try {
            val uid = requireUserId()
            require(investmentId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("investments").document(investmentId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "actualReturnKz" to actualReturnKz.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteInvestment(investmentId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/investments/$investmentId"
        return try {
            val uid = requireUserId()
            require(investmentId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("investments").document(investmentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveReport(report: PeriodReport): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("reports").document()
            docPath = docRef.path
            val payload = mapOf(
                "periodType" to report.periodType,
                "periodLabel" to report.periodLabel.trim().take(80).ifBlank { "Período" },
                "totalRevenueKz" to report.totalRevenueKz.coerceAtLeast(0.0),
                "totalExpenseKz" to report.totalExpenseKz.coerceAtLeast(0.0),
                "netProfitKz" to report.netProfitKz,
                "summary" to report.summary.trim().take(2500).ifBlank { "Sem resumo." },
                "problemsIdentified" to report.problemsIdentified.take(10).map { it.take(200) },
                "opportunities" to report.opportunities.take(10).map { it.take(200) },
                "actionPlan" to report.actionPlan.take(10).map { it.take(200) },
                "generatedByAi" to report.generatedByAi,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reports/$reportId"
        return try {
            val uid = requireUserId()
            require(reportId.isNotBlank())
            val docRef = db.collection("users").document(uid)
                .collection("reports").document(reportId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
