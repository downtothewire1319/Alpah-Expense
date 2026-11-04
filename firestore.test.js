const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
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

// Test 1: Unauthenticated access rejected
test("Unauthenticated user: cannot read public expenses or private expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_expenses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("private_expenses").get());
});

// Test 2: Alice creates and reads public expense
test("Alice: can create and read public expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("public_expenses").doc("pub_1");
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      creatorName: "Alice",
      creatorEmail: "alice@example.com",
      expenseType: "public",
      category: "Groceries",
      title: "Weekly Grocery",
      amount: 450.5,
      date: "2026-11-02",
      paymentMethod: "Cash",
      notes: "Supermarket shopping",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.get());
});

// Test 3: Bob can read Alice's public expense, but cannot update or delete it
test("Bob: can read public expense but cannot modify or delete Alice's expense", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("public_expenses").doc("pub_1").set({
      userId: ALICE_UID,
      creatorName: "Alice",
      creatorEmail: "alice@example.com",
      expenseType: "public",
      category: "Groceries",
      title: "Weekly Grocery",
      amount: 450.5,
      date: "2026-11-02",
      paymentMethod: "Cash",
      notes: "Supermarket shopping",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const docRef = bobDb.collection("public_expenses").doc("pub_1");
  await assertSucceeds(docRef.get());

  await assertFails(
    docRef.update({
      title: "Bob Hacked Grocery",
    })
  );

  await assertFails(docRef.delete());
});

// Test 4: Alice can manage her own private expenses
test("Alice: can create, read, update, and delete her private expenses", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const privRef = aliceDb.collection("users").doc(ALICE_UID).collection("private_expenses").doc("priv_1");

  await assertSucceeds(
    privRef.set({
      userId: ALICE_UID,
      expenseType: "private",
      category: "Personal",
      title: "Watch",
      amount: 2500,
      date: "2026-11-02",
      paymentMethod: "Card",
      notes: "Personal gift",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(privRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("private_expenses").get());
});

// Test 5: Bob CANNOT read or write Alice's private expenses (Zero-Trust Isolation)
test("Bob: CANNOT read, list, or write Alice's private expenses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("private_expenses").doc("priv_1").set({
      userId: ALICE_UID,
      expenseType: "private",
      category: "Personal",
      title: "Secret purchase",
      amount: 9999,
      date: "2026-11-02",
      paymentMethod: "Cash",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const alicePrivRef = bobDb.collection("users").doc(ALICE_UID).collection("private_expenses").doc("priv_1");
  await assertFails(alicePrivRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private_expenses").get());

  await assertFails(
    alicePrivRef.set({
      userId: BOB_UID,
      expenseType: "private",
      category: "Personal",
      title: "Attack",
      amount: 10,
      date: "2026-11-02",
      paymentMethod: "Cash",
      createdAt: new Date(),
    })
  );
});

// Test 6: User Security PIN isolation
test("Alice can manage security PIN settings; Bob cannot read Alice's PIN", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const pinRef = aliceDb.collection("users").doc(ALICE_UID).collection("security").doc("pin");

  await assertSucceeds(
    pinRef.set({
      userId: ALICE_UID,
      hashedPin: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      pinSalt: "randomsalt123",
      autoLockEnabled: true,
      autoLockMinutes: 5,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const bobAccessToAlicePin = bobDb.collection("users").doc(ALICE_UID).collection("security").doc("pin");
  await assertFails(bobAccessToAlicePin.get());
});
