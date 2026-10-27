const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_neo";

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

test("Unauthenticated user: cannot read users or tickets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("tickets").doc("ticket_1").get());
});

test("Authenticated user: can read and create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice Smith",
      phone: "+91 99999 88888",
      role: "CUSTOMER",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      fullName: "Bob Jones",
      role: "CUSTOMER",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: cannot escalate role to ADMIN on profile creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice Smith",
      role: "ADMIN",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create and read own ticket", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ticketData = {
    ticketNumber: "NEO-2026-001",
    userId: ALICE_UID,
    customerId: ALICE_UID,
    customerName: "Alice Smith",
    customerPhone: "+91 99999 88888",
    customerEmail: "alice@example.com",
    serviceType: "In-Store",
    deviceName: "Dell XPS 15",
    issueDescription: "Screen flickering",
    locationAddress: "123 Tech Park",
    status: "PENDING",
    paymentStatus: "PENDING",
    createdAt: new Date(),
  };

  await assertSucceeds(aliceDb.collection("tickets").doc("ticket_1").set(ticketData));
  await assertSucceeds(aliceDb.collection("tickets").doc("ticket_1").get());
});

test("Cross-user: Bob cannot read Alice's ticket if not assigned technician or admin", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tickets").doc("ticket_alice").set({
      ticketNumber: "NEO-2026-002",
      userId: ALICE_UID,
      customerId: ALICE_UID,
      customerName: "Alice Smith",
      customerPhone: "+91 99999 88888",
      deviceName: "MacBook Pro",
      status: "PENDING",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("tickets").doc("ticket_alice").get());
});

test("Assigned technician: can read ticket assigned to them", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tickets").doc("ticket_assigned").set({
      ticketNumber: "NEO-2026-003",
      userId: ALICE_UID,
      customerId: ALICE_UID,
      customerName: "Alice Smith",
      customerPhone: "+91 99999 88888",
      deviceName: "Lenovo ThinkPad",
      status: "IN_PROGRESS",
      assignedTechnicianId: BOB_UID,
      assignedTechnicianName: "Bob Jones",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("tickets").doc("ticket_assigned").get());
});

test("Admin: can read any ticket and manage company profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "ADMIN",
    });
    await context.firestore().collection("tickets").doc("ticket_private").set({
      ticketNumber: "NEO-2026-004",
      userId: ALICE_UID,
      customerId: ALICE_UID,
      customerName: "Alice Smith",
      customerPhone: "+91 99999 88888",
      deviceName: "HP Pavilion",
      status: "PENDING",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(adminDb.collection("tickets").doc("ticket_private").get());

  await assertSucceeds(
    adminDb.collection("company_profile").doc("default").set({
      companyName: "NEO IT TECH",
      phone: "+91 98765 43210",
      email: "info.neoittech@gmail.com",
    })
  );
});
