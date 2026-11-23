const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_creator_2";

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

test("Unauthenticated users cannot read or write profiles, designs, projects, or posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      handle: "@hacker",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Profile + Private PII isolation: owner writes succeed, cross-user PII & profile edits fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Studio",
      handle: "@alice_ss",
      bio: "CapCut & Canva creator",
      photoUrl: "https://example.com/a.jpg",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Public profile visible to other signed-in users for TikTok attribution; private PII denied
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get());

  // Bob cannot edit Alice's displayName or bio, nor inject ghost fields
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated",
      isVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can update her own profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Pro",
      bio: "1080p+ Editor",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Skewed",
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Cloud CapCut projects and Canva Pro designs stay strictly isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("designs").doc("design_1").set({
      userId: ALICE_UID,
      title: "Neon Poster",
      category: "Poster",
      aspectLabel: "3:4",
      bgHex: 0xFF0B1018,
      accentHex: 0xFF00E5FF,
      headline: "DROP LIVE",
      designJson: "{\"elements\":[]}",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudProjects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "Reel Edit",
      aspectLabel: "9:16",
      resolutionLabel: "1080P+",
      clipCount: 2,
      totalDurationMs: 6500,
      filterLabel: "A1 Studio",
      projectJson: "{\"clips\":[]}",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("designs").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cloudProjects").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("designs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cloudProjects").get());

  // Reject `@DocumentId` id field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("designs").doc("design_2").set({
      id: "design_2",
      userId: ALICE_UID,
      title: "Bad Payload",
      category: "Poster",
      aspectLabel: "1:1",
      bgHex: 0xFF000000,
      accentHex: 0xFFFFFFFF,
      designJson: "{}",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("TikTok Posts & Comments: filtered public query, likes, comments, and cross-user protections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice Studio",
      authorHandle: "@alice_ss",
      caption: "New CapCut Pro + Canva Reel!",
      hashtags: ["#ssstudio", "#capcutpro"],
      soundTitle: "Neon Pulse Beat",
      mediaType: "TEMPLATE_REEL",
      mediaUrl: "https://example.com/reel.mp4",
      accentHex: 0xFF00E5FF,
      filterLabel: "A1 Studio",
      templateId: "tpl_neon_pulse",
      visibility: "PUBLIC",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      savesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered PUBLIC query succeeds; unfiltered query fails Query Enforcer
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("posts").get());

  // Bob can like/increment engagement counters + post comment + track own likedPosts
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("likedPosts").doc("post_1").set({
      targetId: "post_1",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      authorHandle: "@bob",
      text: "Fire edit!",
      visibility: "PUBLIC",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").where("visibility", "==", "PUBLIC").get()
  );

  // Bob cannot hijack Alice's caption or delete Alice's post
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Hijacked!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
});
