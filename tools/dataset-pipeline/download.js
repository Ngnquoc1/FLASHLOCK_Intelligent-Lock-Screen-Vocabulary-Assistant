const fs = require("fs");
const path = require("path");
const https = require("https");

function parseArgs() {
  const args = process.argv.slice(2);
  const configIndex = args.indexOf("--config");
  const configPath = configIndex >= 0 ? args[configIndex + 1] : null;
  if (!configPath) {
    throw new Error("Missing --config path");
  }
  return { configPath };
}

function loadConfig(configPath) {
  const raw = fs.readFileSync(configPath, "utf8");
  return JSON.parse(raw);
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function download(url, destPath) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(destPath);
    https.get(url, (response) => {
      if (response.statusCode !== 200) {
        reject(new Error(`Download failed: ${response.statusCode} ${response.statusMessage}`));
        return;
      }
      response.pipe(file);
      file.on("finish", () => file.close(resolve));
    }).on("error", (err) => {
      fs.unlink(destPath, () => reject(err));
    });
  });
}

function fileNameFromUrl(url) {
  return url.split("/").pop();
}

async function run() {
  const { configPath } = parseArgs();
  const config = loadConfig(configPath);

  const rawDir = config.rawDir;
  if (!rawDir) {
    throw new Error("rawDir is required in config");
  }
  ensureDir(rawDir);

  const tasks = [];

  if (config.wortUrl) {
    const dest = path.join(rawDir, fileNameFromUrl(config.wortUrl));
    tasks.push(download(config.wortUrl, dest).then(() => ({ name: "wort", dest })));
  } else {
    console.log("Skipped wortUrl (missing)");
  }

  if (config.kaikkiUrl) {
    const dest = path.join(rawDir, fileNameFromUrl(config.kaikkiUrl));
    tasks.push(download(config.kaikkiUrl, dest).then(() => ({ name: "kaikki", dest })));
  } else {
    console.log("Skipped kaikkiUrl (missing)");
  }

  if (tasks.length === 0) {
    console.log("No downloads configured.");
    return;
  }

  const results = await Promise.all(tasks);
  for (const result of results) {
    console.log(`Downloaded ${result.name} -> ${result.dest}`);
  }
}

run().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

