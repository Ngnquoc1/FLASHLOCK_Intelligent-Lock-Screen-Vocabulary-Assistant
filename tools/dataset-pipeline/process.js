const fs = require("fs");
const path = require("path");
const readline = require("readline");
const zlib = require("zlib");

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

function openStream(filePath) {
  const rawStream = fs.createReadStream(filePath);
  if (filePath.endsWith(".gz")) {
    return rawStream.pipe(zlib.createGunzip());
  }
  return rawStream;
}

async function readJsonLines(filePath, onEntry) {
  const stream = openStream(filePath);
  const rl = readline.createInterface({ input: stream, crlfDelay: Infinity });
  for await (const line of rl) {
    if (!line.trim()) continue;
    try {
      const entry = JSON.parse(line);
      onEntry(entry);
    } catch (err) {
      // Skip malformed lines.
    }
  }
}

function normalizeTerm(term) {
  return term ? term.trim() : "";
}

function pickFirstString(values) {
  if (!Array.isArray(values)) return null;
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return null;
}

function extractWort(entry) {
  const term = normalizeTerm(entry.term || entry.word || entry.lemma || entry.entry || "");
  if (!term) return null;

  let definition = null;
  if (Array.isArray(entry.definitions)) {
    definition = pickFirstString(entry.definitions);
  }
  if (!definition && Array.isArray(entry.meanings)) {
    definition = pickFirstString(entry.meanings.map((m) => m.definition || m.meaning || m.text));
  }
  if (!definition && Array.isArray(entry.senses)) {
    definition = pickFirstString(entry.senses.map((s) => s.definition || s.gloss || s.text));
  }

  const example = entry.example || pickFirstString(entry.examples || []);
  const pronunciation = entry.pronunciation || entry.ipa || pickFirstString(entry.pronunciations || []);
  const wordType = entry.wordType || entry.partOfSpeech || entry.pos || entry.type;

  return {
    term,
    definition: definition || "",
    example: example || "",
    pronunciation: pronunciation || "",
    wordType: wordType || "",
    language: "en"
  };
}

function extractKaikki(entry) {
  const term = normalizeTerm(entry.word || entry.term || "");
  if (!term) return null;

  let translations = [];
  const senses = Array.isArray(entry.senses) ? entry.senses : [];
  for (const sense of senses) {
    const trs = Array.isArray(sense.translations) ? sense.translations : [];
    for (const tr of trs) {
      const lang = tr.lang || tr.language || tr.lang_name;
      const code = tr.lang_code || tr.langCode;
      if (code === "vi" || lang === "Vietnamese" || lang === "Tiếng Việt") {
        const word = tr.word || tr.translation || tr.text;
        if (typeof word === "string" && word.trim()) {
          translations.push(word.trim());
        }
      }
    }
  }

  translations = Array.from(new Set(translations));
  if (translations.length === 0) return null;

  return {
    term,
    definition: translations.join("; "),
    example: "",
    pronunciation: entry.pronunciation || entry.ipa || "",
    wordType: entry.pos || entry.partOfSpeech || "",
    language: "vi"
  };
}

function extractKaikkiEnglish(entry) {
  const term = normalizeTerm(entry.word || entry.term || "");
  if (!term) return null;

  const senses = Array.isArray(entry.senses) ? entry.senses : [];
  const glosses = senses.length > 0 ? senses[0].glosses || senses[0].raw_glosses : null;
  const definition = Array.isArray(glosses) ? pickFirstString(glosses) : null;
  if (!definition) return null;

  let example = "";
  if (Array.isArray(senses[0].examples)) {
    const firstExample = senses[0].examples.find((ex) => ex.type === "example" || ex.type === "") || senses[0].examples[0];
    if (firstExample && typeof firstExample.text === "string") {
      example = firstExample.text.trim();
    }
  }

  let pronunciation = "";
  if (Array.isArray(entry.sounds)) {
    const sound = entry.sounds.find((s) => typeof s.ipa === "string" && s.ipa.trim());
    if (sound) pronunciation = sound.ipa.trim();
  }

  const wordType = entry.pos || entry.partOfSpeech || "";

  return {
    term,
    definition,
    example,
    pronunciation,
    wordType,
    language: "en"
  };
}

function extractKaikkiVietnamese(entry) {
  const term = normalizeTerm(entry.word || entry.term || "");
  if (!term) return null;

  let translations = [];
  const senses = Array.isArray(entry.senses) ? entry.senses : [];
  for (const sense of senses) {
    const trs = Array.isArray(sense.translations) ? sense.translations : [];
    for (const tr of trs) {
      const lang = tr.lang || tr.language || tr.lang_name;
      const code = tr.lang_code || tr.langCode;
      if (code === "vi" || lang === "Vietnamese" || lang === "Tiếng Việt") {
        const word = tr.word || tr.translation || tr.text;
        if (typeof word === "string" && word.trim()) {
          translations.push(word.trim());
        }
      }
    }
  }

  translations = Array.from(new Set(translations));
  if (translations.length === 0) return null;

  return {
    term,
    definition: translations.join("; "),
    example: "",
    pronunciation: "",
    wordType: entry.pos || entry.partOfSpeech || "",
    language: "vi"
  };
}

function loadTopicRules(rulePath) {
  const raw = fs.readFileSync(rulePath, "utf8");
  return JSON.parse(raw);
}

function matchesRule(term, rule) {
  const filter = rule.filter || {};
  if (filter.type === "prefix" && Array.isArray(filter.ranges)) {
    const first = term[0]?.toLowerCase() || "";
    for (const range of filter.ranges) {
      const [start, end] = range.split("-");
      if (first >= start && first <= end) return true;
    }
    return false;
  }
  return true;
}

function buildTopics(wordsByLang, rules, maxPerTopic) {
  const topics = [];
  const words = [];

  for (const rule of rules) {
    const pool = wordsByLang[rule.language] || [];
    const selected = [];
    for (const word of pool) {
      if (selected.length >= maxPerTopic) break;
      if (matchesRule(word.term, rule)) {
        selected.push(word);
      }
    }

    topics.push({
      topicId: rule.id,
      title: rule.title,
      category: rule.language === "vi" ? "VI" : "EN",
      thumbnailUrl: "",
      wordCount: selected.length,
      createdAt: new Date().toISOString(),
      language: rule.language
    });

    for (const word of selected) {
      words.push({
        wordId: `${rule.id}_${word.term.toLowerCase().replace(/\s+/g, "_")}`,
        topicId: rule.id,
        term: word.term,
        definition: word.definition,
        example: word.example,
        pronunciation: word.pronunciation,
        wordType: word.wordType,
        status: "NEW",
        createdAt: new Date().toISOString(),
        language: rule.language
      });
    }
  }

  return { topics, words };
}

async function run() {
  const { configPath } = parseArgs();
  const config = loadConfig(configPath);
  const processedDir = config.processedDir;
  const topicRulesPath = config.topicRulesPath;
  const limit = Number(config.limit || 0);
  const useKaikkiForEnglish = Boolean(config.useKaikkiForEnglish);

  if (!processedDir) {
    throw new Error("processedDir is required in config");
  }
  if (!topicRulesPath) {
    throw new Error("topicRulesPath is required in config");
  }

  ensureDir(processedDir);

  const rawDir = config.rawDir;
  if (!rawDir) throw new Error("rawDir is required in config");

  const wortFile = config.wortFile || findFile(rawDir, "wort") || findFile(rawDir, "wordnet");
  const kaikkiFile = config.kaikkiFile || findFile(rawDir, "kaikki");

  if (!kaikkiFile) {
    throw new Error("Cannot find Kaikki file in rawDir. Set kaikkiFile in config.");
  }
  if (!useKaikkiForEnglish && !wortFile) {
    throw new Error("Cannot find Wort file in rawDir. Set wortFile in config or enable useKaikkiForEnglish.");
  }

  const english = [];
  const vietnamese = [];

  if (useKaikkiForEnglish) {
    await readJsonLines(kaikkiFile, (entry) => {
      if (limit && english.length >= limit && vietnamese.length >= limit) return;
      if (!limit || english.length < limit) {
        const normalizedEn = extractKaikkiEnglish(entry);
        if (normalizedEn) english.push(normalizedEn);
      }
      if (!limit || vietnamese.length < limit) {
        const normalizedVi = extractKaikkiVietnamese(entry);
        if (normalizedVi) vietnamese.push(normalizedVi);
      }
    });
  } else {
    await readJsonLines(wortFile, (entry) => {
      if (limit && english.length >= limit) return;
      const normalized = extractWort(entry);
      if (normalized && normalized.definition) {
        english.push(normalized);
      }
    });

    await readJsonLines(kaikkiFile, (entry) => {
      if (limit && vietnamese.length >= limit) return;
      const normalized = extractKaikkiVietnamese(entry);
      if (normalized && normalized.definition) {
        vietnamese.push(normalized);
      }
    });
  }

  const rules = loadTopicRules(topicRulesPath);
  const { topics, words } = buildTopics({ en: english, vi: vietnamese }, rules, config.maxPerTopic || 500);

  fs.writeFileSync(path.join(processedDir, "topics.seed.json"), JSON.stringify(topics, null, 2));
  fs.writeFileSync(path.join(processedDir, "words.seed.json"), JSON.stringify(words, null, 2));

  console.log(`Saved topics: ${topics.length}`);
  console.log(`Saved words: ${words.length}`);
}

function findFile(rawDir, keyword) {
  const files = fs.readdirSync(rawDir);
  const match = files.find((file) => file.toLowerCase().includes(keyword));
  return match ? path.join(rawDir, match) : null;
}

run().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
