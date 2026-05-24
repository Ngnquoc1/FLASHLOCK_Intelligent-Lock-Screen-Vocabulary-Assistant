# Week 2/4 Report (Vocabulary + Topics + Library)

Ngay cap nhat: 2026-05-18

## 1) Tong quan nhung gi da lam
- Hoan thien data layer cho Topic/Word (model, repository, datasource Firebase).
- Hoan thien UI danh sach tu vung + tab Topics + bottom sheet them/sua.
- Them dataset seed cho topics/words (IELTS + IT).
- Bo sung strings/values-vi de ho tro song ngu (EN mac dinh, VI khi may de tieng Viet).
- Fix va toi uu search tu vung (debounce + fallback khi thieu termLower).
- Bo sung thong bao loi, empty state, dialog xoa.
- Co test co ban cho VocabularyViewModel.

## 2) Chi tiet nhung gi da them/doi
### Data layer
- `app/src/main/java/com/nhom18/flashlock/data/model/Topic.java`
- `app/src/main/java/com/nhom18/flashlock/data/model/Word.java`
- `app/src/main/java/com/nhom18/flashlock/data/remote/FirebaseTopicDataSource.java`
- `app/src/main/java/com/nhom18/flashlock/data/remote/FirebaseWordDataSource.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/FirebaseTopicRepository.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/FirebaseWordRepository.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/TopicRepository.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/WordRepository.java`

### UI/UX
- Vocabulary
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/VocabularyFragment.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/VocabularyViewModel.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/VocabularyAdapter.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/WordAdapter.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/TopicAdapter.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/vocabulary/WordFilter.java`
  - `app/src/main/res/layout/fragment_vocabulary.xml`
  - `app/src/main/res/layout/item_vocabulary.xml`
  - `app/src/main/res/layout/item_word.xml`
  - `app/src/main/res/layout/item_topic.xml`
  - `app/src/main/res/layout/bottom_sheet_add_word.xml`
- Library
  - `app/src/main/java/com/nhom18/flashlock/ui/library/LibraryFragment.java`
  - `app/src/main/java/com/nhom18/flashlock/ui/library/TopicAdapter.java`
- Main/Navigation
  - `app/src/main/java/com/nhom18/flashlock/ui/main/MainActivity.java`
  - `app/src/main/res/layout/activity_homebatch.xml`

### Drawables/Styles
- `app/src/main/res/drawable/*` (bg, icons, chip states, tab states, v.v.)
- `app/src/main/res/values/colors.xml` (cap nhat mau theo UI)

### Strings & localization
- `app/src/main/res/values/strings.xml` (EN mac dinh)
- `app/src/main/res/values-vi/strings.xml` (VI)
- Layout co text hardcode da chuyen sang @string:
  - `app/src/main/res/layout/activity_splash.xml`
  - `app/src/main/res/layout/activity_homebatch.xml`

### Dataset seed
- `app/DOCS/topics.seed.json`
- `app/DOCS/words.seed.json` (IELTS + IT)

### Firebase/Config/Docs
- `.firebaserc`
- `firebase.json`
- `firestore.indexes.json`
- `app/DOCS/ExecutionPlan.End2End.4Weeks.md`
- `app/DOCS/ExecutionPlan.Timeline.vi.md`

### Test
- `app/src/test/java/com/nhom18/flashlock/ui/vocabulary/VocabularyViewModelTest.java`

## 3) Tinh nang da co
- Quan ly tu vung: hien danh sach, them/sua/xoa tu.
- Filter theo trang thai: All / New / Learning / Mastered.
- Search tu vung:
  - Server-side query theo termLower.
  - Debounce 300ms.
  - Fallback local filter khi termLower chua co.
- Tab Topics trong man Vocabulary.
- Dialog xac nhan xoa tu.
- Empty state + error state.
- Localization EN/VI (tu dong theo ngon ngu he thong).

## 4) Nhung gi con thieu (Week 2/4)
1) **LibraryFragment chinh thuc**: noi dung chua du (hien dang rong / toi thieu).
2) **Seed dataset hoan chinh**: can bo sung them chu de IELTS + IT (so luong va phan loai day du hon).
3) **Mapping/luu wordType day du**: can dam bao day du qua UI -> ViewModel -> DataSource -> Firestore.
4) **Server-side search toi uu hon**: neu dataset lon, can index + query theo nhieu truong (term/definition).
5) **Test mo rong**: them test cho filter/search/add/edit/delete flow.
6) **Loading/Error state toan bo**: thong diep loi theo ma loi ro rang hon (mapping chi tiet neu can).

## 5) Luu y ky thuat
- Search duoc toi uu bang debounce va fallback local.
- Neu Firestore chua co field `termLower`, can backfill du lieu cu hoac se dung fallback local.
- Strings: neu key khong co trong `values-vi`, Android se fallback ve `values`.

## 6) Ghi chu ve dataset
- Hien co seed IELTS + IT trong `app/DOCS/words.seed.json`.
- Can them `termLower` neu muon search chuan theo server ngay lap tuc.

---
Neu can, toi co the:
- Tao file backfill termLower.
- Bo sung LibraryFragment hoan chinh.
- Viet them test cho search/filter.

