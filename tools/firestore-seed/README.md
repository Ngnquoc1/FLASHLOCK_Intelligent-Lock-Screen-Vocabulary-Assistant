# Firestore Seed (FlashLock)

This tool imports topics and user words into Firestore using JSON seed files.

## Data sources

- Topics: `app/DOCS/topics.seed.json`
- Words: `app/DOCS/words.seed.json`

## Quick dry run (no Firebase required)

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\firestore-seed"
node seed.js --dry-run --limit 3
Pop-Location
```

## Install dependencies

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\firestore-seed"
npm install
Pop-Location
```

## Seed topics (Firestore)

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\firestore-seed"
node seed.js --project <PROJECT_ID> --service-account <PATH_TO_SERVICE_ACCOUNT_JSON> --topics-only
Pop-Location
```

## Seed words for a user

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\firestore-seed"
node seed.js --project <PROJECT_ID> --service-account <PATH_TO_SERVICE_ACCOUNT_JSON> --uid <USER_UID> --words-only
Pop-Location
```

## Seed shared topic words (public library)

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\firestore-seed"
node seed.js --project <PROJECT_ID> --service-account <PATH_TO_SERVICE_ACCOUNT_JSON> --words "C:\STUDY\FlashLock2\data\processed\words.seed.json" --topic-words --words-only
Pop-Location
```

## Options

- `--topics <path>`: custom topics JSON file
- `--words <path>`: custom words JSON file
- `--uid <userId>`: target user for `my_words`
- `--limit <n>`: limit number of records per type
- `--dry-run`: validate JSON without writing
- `--no-merge`: overwrite documents instead of merge
