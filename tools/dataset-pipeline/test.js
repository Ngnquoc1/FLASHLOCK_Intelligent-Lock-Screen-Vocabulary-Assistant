const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const root = path.resolve(__dirname, "..", "..");
const rawDir = path.join(root, "data", "raw-test");
const processedDir = path.join(root, "data", "processed-test");
const fixturesDir = path.join(__dirname, "fixtures");

fs.mkdirSync(rawDir, { recursive: true });
fs.mkdirSync(processedDir, { recursive: true });

fs.copyFileSync(path.join(fixturesDir, "wort.sample.jsonl"), path.join(rawDir, "wort.sample.jsonl"));
fs.copyFileSync(path.join(fixturesDir, "kaikki.sample.jsonl"), path.join(rawDir, "kaikki.sample.jsonl"));

const configPath = path.join(__dirname, "config.test.json");
fs.writeFileSync(
  configPath,
  JSON.stringify(
    {
      rawDir,
      processedDir,
      topicRulesPath: path.join(__dirname, "topic-rules.example.json"),
      wortFile: path.join(rawDir, "wort.sample.jsonl"),
      kaikkiFile: path.join(rawDir, "kaikki.sample.jsonl"),
      useKaikkiForEnglish: true,
      maxPerTopic: 2,
      limit: 0
    },
    null,
    2
  )
);

execSync(`node "${path.join(__dirname, "process.js")}" --config "${configPath}"`, {
  stdio: "inherit"
});

const topics = JSON.parse(fs.readFileSync(path.join(processedDir, "topics.seed.json"), "utf8"));
const words = JSON.parse(fs.readFileSync(path.join(processedDir, "words.seed.json"), "utf8"));

if (!Array.isArray(topics) || topics.length === 0) {
  throw new Error("No topics generated");
}
if (!Array.isArray(words) || words.length === 0) {
  throw new Error("No words generated");
}

console.log("Test passed.");
