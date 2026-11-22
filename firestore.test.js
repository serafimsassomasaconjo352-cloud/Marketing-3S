const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_rumo_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      title: "Receita Teste",
      amountKz: 100000,
      type: "REVENUE",
      category: "Serviços IA",
      transactionDateMillis: Date.now() - 1000,
      recurring: true,
      settled: true,
      wastefulFlag: false,
      personalSeparated: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner: financial_plan create, partial update, and ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("financial_plan").set({
      companyName: "RUMO 3S IA",
      monthlyRevenueGoalKz: 2500000,
      monthlyExpenseBudgetKz: 1200000,
      annualRevenueGoalKz: 30000000,
      emergencyReserveTargetKz: 3600000,
      emergencyReserveBalanceKz: 900000,
      openingCapitalKz: 1500000,
      notes: "Plano trimestral Luanda",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("financial_plan").update({
      monthlyRevenueGoalKz: 3000000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Wrong settingId rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("other_plan").set({
      companyName: "RUMO 3S IA",
      monthlyRevenueGoalKz: 100,
      monthlyExpenseBudgetKz: 100,
      annualRevenueGoalKz: 100,
      emergencyReserveTargetKz: 100,
      emergencyReserveBalanceKz: 0,
      openingCapitalKz: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Ghost field / immutable createdAt change rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("financial_plan").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner: transactions create, update, list, delete, and validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create transaction matching Kotlin payload (NO 'id' key)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      title: "Contrato Automação IA",
      amountKz: 450000.0,
      type: "REVENUE",
      category: "Serviços IA",
      notes: "Cliente B2B Luanda",
      transactionDateMillis: Date.now() - 2000,
      recurring: true,
      settled: true,
      wastefulFlag: false,
      personalSeparated: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List subcollection succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // Partial update (e.g., settle debt)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      settled: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject 'id' field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      id: "tx_bad",
      title: "Bad",
      amountKz: 5000,
      type: "REVENUE",
      category: "Outro",
      transactionDateMillis: Date.now() - 1000,
      recurring: false,
      settled: true,
      wastefulFlag: false,
      personalSeparated: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_future").set({
      title: "Future",
      amountKz: 5000,
      type: "REVENUE",
      category: "Outro",
      transactionDateMillis: Date.now() - 1000,
      recurring: false,
      settled: true,
      wastefulFlag: false,
      personalSeparated: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete()
  );
});

test("Owner: investments & reports CRUD and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investments").doc("inv_1").set({
      title: "Funil B2B WhatsApp",
      capitalKz: 120000,
      expectedReturnKz: 480000,
      actualReturnKz: 0,
      riskLevel: "LOW",
      focusType: "PARETO_80_20",
      status: "ACTIVE",
      pareto8020: true,
      justification: "Foco nos 20% clientes de maior margem",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").set({
      periodType: "MONTHLY",
      periodLabel: "Novembro 2026",
      totalRevenueKz: 1200000,
      totalExpenseKz: 500000,
      netProfitKz: 700000,
      summary: "Resultado positivo com margem sólida.",
      problemsIdentified: ["Gasto elevado em licenças não utilizadas"],
      opportunities: ["Pacotes mensais recorrentes B2B"],
      actionPlan: ["Renegociar fornecedores e reforçar reserva"],
      generatedByAi: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reports cannot be mutated
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").update({
      summary: "Tampered",
    })
  );

  // Bob cannot read or write Alice's data
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("investments").doc("inv_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("investments").doc("inv_1").update({
      status: "DISCARDED",
    })
  );
});
