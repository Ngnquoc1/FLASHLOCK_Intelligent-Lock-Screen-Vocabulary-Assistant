const fs = require("fs");
const path = require("path");

const ROOT_DIR = path.resolve(__dirname, "..", "..");
const DEFAULT_TOPICS = path.resolve(ROOT_DIR, "app", "DOCS", "topics.seed.json");
const DEFAULT_WORDS = path.resolve(ROOT_DIR, "app", "DOCS", "words.seed.json");

function hasFlag(flag) {
  return process.argv.includes(flag);
}

function getArg(flag, fallback) {
  const index = process.argv.indexOf(flag);
  if (index === -1 || index + 1 >= process.argv.length) {
    return fallback;
  }
  return process.argv[index + 1];
}

function parseNumber(value) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function readJson(filePath) {
  const raw = fs.readFileSync(filePath, "utf8");
  return JSON.parse(raw);
}

function normalizeDate(value) {
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function buildTopicPayload(topic, admin) {
  const payload = {};
  if (topic.title) payload.title = topic.title;
  if (topic.category) payload.category = topic.category;
  if (topic.thumbnailUrl) payload.thumbnailUrl = topic.thumbnailUrl;
  if (typeof topic.wordCount === "number") payload.wordCount = topic.wordCount;
  if (topic.language) payload.language = topic.language;

  const createdAt = normalizeDate(topic.createdAt);
  if (admin) {
    payload.createdAt = createdAt
      ? admin.firestore.Timestamp.fromDate(createdAt)
      : admin.firestore.FieldValue.serverTimestamp();
  } else if (createdAt) {
    payload.createdAt = createdAt.toISOString();
  }

  payload.topicId = topic.topicId || null;
  return payload;
}

function buildWordPayload(word, admin) {
  const payload = {};
  if (word.term) {
    payload.term = word.term;
    payload.termLower = word.term.trim().toLowerCase();
  }
  if (word.definition) payload.definition = word.definition;
  if (word.example) payload.example = word.example;
  if (word.pronunciation) payload.pronunciation = word.pronunciation;
  if (word.wordType) payload.wordType = word.wordType;
  if (word.topicId) payload.topicId = word.topicId;
  if (word.language) payload.language = word.language;
  payload.status = word.status || "NEW";

  const createdAt = normalizeDate(word.createdAt);
  if (admin) {
    payload.createdAt = createdAt
      ? admin.firestore.Timestamp.fromDate(createdAt)
      : admin.firestore.FieldValue.serverTimestamp();
  } else if (createdAt) {
    payload.createdAt = createdAt.toISOString();
  }

  payload.wordId = word.wordId || null;
  return payload;
}

function applyLimit(items, limit) {
  if (!limit || limit <= 0) {
    return items;
  }
  return items.slice(0, limit);
}

function printHelp() {
  console.log("Usage: node seed.js [options]");
  console.log("");
  console.log("Options:");
  console.log("  --topics <path>        Path to topics JSON");
  console.log("  --words <path>         Path to words JSON");
  console.log("  --uid <userId>         Target user id for words (user mode)");
  console.log("  --service-account <p>  Firebase service account JSON");
  console.log("  --project <id>         Firebase project id");
  console.log("  --topics-only          Only import topics");
  console.log("  --words-only           Only import words");
  console.log("  --topic-words          Import words into topics/{topicId}/words");
  console.log("  --limit <n>            Limit number of records per type");
  console.log("  --dry-run              Validate and print summary only");
  console.log("  --no-merge             Overwrite documents instead of merge");
}

async function run() {
  if (hasFlag("--help")) {
    printHelp();
    return;
  }

  const topicsPath = getArg("--topics", DEFAULT_TOPICS);
  const wordsPath = getArg("--words", DEFAULT_WORDS);
  const uid = getArg("--uid", null);
  const serviceAccountPath = getArg("--service-account", null);
  const projectId = getArg("--project", null);
  const limit = parseNumber(getArg("--limit", null));
  const dryRun = hasFlag("--dry-run");
  const merge = !hasFlag("--no-merge");
  const includeTopics = !hasFlag("--words-only");
  const includeWords = !hasFlag("--topics-only");
  const useTopicWords = hasFlag("--topic-words");

  if (includeWords && !uid && !dryRun && !useTopicWords) {
    console.error("Missing --uid for words import.");
    process.exitCode = 1;
    return;
  }

  const topics = includeTopics ? applyLimit(readJson(topicsPath), limit) : [];
  const words = includeWords ? applyLimit(readJson(wordsPath), limit) : [];

  if (dryRun) {
    console.log("Dry run enabled.");
    console.log(`Topics: ${topics.length}, Words: ${words.length}`);
    if (topics.length > 0) {
      console.log("Sample topic:", buildTopicPayload(topics[0], null));
    }
    if (words.length > 0) {
      console.log("Sample word:", buildWordPayload(words[0], null));
    }
    return;
  }

  if (!serviceAccountPath || !projectId) {
    console.error("Missing --service-account or --project for Firestore write.");
    process.exitCode = 1;
    return;
  }

  // Lazy load firebase-admin only when writing.
  const admin = require("firebase-admin");
  const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId
  });

  const db = admin.firestore();

  async function commitBatch(batch) {
    if (!batch) return;
    await batch.commit();
  }

  async function writeTopics() {
    let batch = db.batch();
    let counter = 0;

    for (const topic of topics) {
      const docId = topic.topicId || db.collection("topics").doc().id;
      const payload = buildTopicPayload({ ...topic, topicId: docId }, admin);
      const ref = db.collection("topics").doc(docId);
      batch.set(ref, payload, { merge });
      counter += 1;

      if (counter % 450 === 0) {
        await commitBatch(batch);
        batch = db.batch();
      }
    }

    await commitBatch(batch);
    console.log(`Imported ${counter} topics.`);
  }

  async function writeWordsToUser() {
    let batch = db.batch();
    let counter = 0;
    const wordsRef = db.collection("users").doc(uid).collection("my_words");

    for (const word of words) {
      const docId = word.wordId || wordsRef.doc().id;
      const payload = buildWordPayload({ ...word, wordId: docId }, admin);
      const ref = wordsRef.doc(docId);
      batch.set(ref, payload, { merge });
      counter += 1;

      if (counter % 450 === 0) {
        await commitBatch(batch);
        batch = db.batch();
      }
    }

    await commitBatch(batch);
    console.log(`Imported ${counter} words to users/${uid}/my_words.`);
  }

  async function writeWordsToTopics() {
    let batch = db.batch();
    let counter = 0;

    for (const word of words) {
      const topicId = word.topicId;
      if (!topicId) {
        continue;
      }
      const topicRef = db.collection("topics").doc(topicId).collection("words");
      const docId = word.wordId || topicRef.doc().id;
      const payload = buildWordPayload({ ...word, wordId: docId }, admin);
      const ref = topicRef.doc(docId);
      batch.set(ref, payload, { merge });
      counter += 1;

      if (counter % 450 === 0) {
        await commitBatch(batch);
        batch = db.batch();
      }
    }

    await commitBatch(batch);
    console.log(`Imported ${counter} words to topics/{topicId}/words.`);
  }

  if (includeTopics) {
    await writeTopics();
  }

  if (includeWords) {
    if (useTopicWords) {
      await writeWordsToTopics();
    } else {
      await writeWordsToUser();
    }
  }
}

run().catch((error) => {
  console.error("Seed failed:", error);
  process.exitCode = 1;
});
