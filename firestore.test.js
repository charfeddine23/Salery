const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated user: denied read/write across all user paths", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      lastSyncedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner: user profile create, partial update, and shadow/cross-user rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      language: "en",
      themeMode: "DARK",
      designStyle: "LIQUID_GLASS",
      lastSyncedAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      localUsername: "AliceDZ",
      lastSyncedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Transactions subcollection: owner create/update/list succeed; extra id or future timestamp fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 85000,
      category: "salary",
      title: "Monthly Salary",
      note: "April",
      dateMillis: Date.now() - 10000,
      salaryCredit: true,
      paidFromSavings: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      amount: 90000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId "id" key inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_2").set({
      id: "tx_2",
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 500,
      category: "food",
      title: "Coffee",
      note: "",
      dateMillis: Date.now() - 10000,
      salaryCredit: false,
      paidFromSavings: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_3").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 500,
      category: "food",
      title: "Future",
      note: "",
      dateMillis: Date.now(),
      salaryCredit: false,
      paidFromSavings: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Debts & vault_settings & encrypted_backups: strict schema and secret exclusion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("debts").doc("debt_1").set({
      userId: ALICE_UID,
      type: "OWED_BY_ME",
      personName: "Karim",
      amount: 12000,
      note: "Loan",
      dueDateMillis: Date.now() + 86400000,
      autoPayOnDueDate: true,
      paid: false,
      createdDateMillis: Date.now() - 10000,
      recurringMonthlyInstallment: false,
      monthlyInstallmentAmount: 0,
      installmentDayOfMonth: 1,
      remainingAmount: 12000,
      lastInstallmentCreditedMonth: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vault_settings").doc("primary").set({
      userId: ALICE_UID,
      salaryAmount: 85000,
      salaryPayday: 25,
      salaryEmployer: "Enterprise",
      salaryAutoCredit: true,
      savingsWalletBalance: 10000,
      savingsGoalAmount: 50000,
      savingsHistoryJson: "[]",
      includeAssetsInSavingsTotal: false,
      goldAssetHoldingsJson: "[]",
      binanceHoldingsJson: "[]",
      cardHubOrderJson: "[\"CLASSIC\",\"VISA\"]",
      classicCardBankStyle: "EDAHABIA",
      classicCardHolderName: "ALICE",
      classicCardExpiry: "11/29",
      visaCardProvider: "DEFAULT",
      visaCardHolderName: "ALICE",
      visaCardExpiry: "08/28",
      thirdCardEnabled: false,
      thirdCardProvider: "BINANCE",
      thirdCardHolderName: "ALICE",
      thirdCardExpiry: "08/28",
      language: "en",
      themeMode: "DARK",
      designStyle: "LIQUID_GLASS",
      stealthMode: false,
      breathingGlowEnabled: true,
      screenshotProtectionEnabled: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject unapproved raw secret fields in vault_settings
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vault_settings").doc("primary").update({
      vaultKeysJson: "[{\"secret\":\"1234\"}]",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Accept encrypted backup envelope only in VAULTIX_ENC_V1 format
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("encrypted_backups").doc("latest").set({
      userId: ALICE_UID,
      format: "VAULTIX_ENC_V1",
      envelopeJson: "{\"vaultixBackupFormat\":\"VAULTIX_ENC_V1\"}",
      appVersion: "1.0.3",
      createdAt: created,
      updatedAt: created,
    })
  );
});
