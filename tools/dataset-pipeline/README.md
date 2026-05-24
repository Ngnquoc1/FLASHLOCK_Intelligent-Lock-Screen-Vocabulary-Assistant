# Dataset Pipeline (Wort + Kaikki EN->VI)

This pipeline downloads and normalizes:
- **Wort** for English definitions/examples
- **Kaikki English** for Vietnamese translations

It outputs `topics.seed.json` and `words.seed.json` in `data/processed/` for use with the Firestore seed tool.

## 1) Configure

Copy the example config and edit URLs:

```powershell
Copy-Item "C:\STUDY\FlashLock2\tools\dataset-pipeline\config.example.json" "C:\STUDY\FlashLock2\tools\dataset-pipeline\config.json"
```

Edit `config.json`:
- `wortUrl`: direct download URL to Wort JSON/JSONL(.gz)
- `kaikkiUrl`: direct download URL to Kaikki English JSONL(.gz)
- `useKaikkiForEnglish`: set true to use Kaikki as the English source
- `wortFile` / `kaikkiFile`: override file names in `data/raw`

## 2) Download datasets

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\dataset-pipeline"
node download.js --config config.json
Pop-Location
```

## 3) Process datasets

```powershell
Push-Location "C:\STUDY\FlashLock2\tools\dataset-pipeline"
node process.js --config config.json
Pop-Location
```

Outputs:
- `data/processed/topics.seed.json`
- `data/processed/words.seed.json`

## 4) Import to Firestore

Use the seed tool in `tools/firestore-seed/`.

## Notes

- Use `useKaikkiForEnglish: true` if you want Kaikki to provide both EN definitions and VI translations.
- WordNet database dumps (data.noun/index.adj) are not supported directly. If you only have `wordnet.json` in a custom schema, add a separate converter first.
