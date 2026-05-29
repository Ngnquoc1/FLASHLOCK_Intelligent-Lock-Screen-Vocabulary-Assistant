# FlashLock app module

## Topic words screen
- Activity: `com.nhom18.flashlock.ui.library.LibraryTopicWordsActivity`
- Layout: `app/src/main/res/layout/activity_library_topic_words.xml`
- Adapter: `com.nhom18.flashlock.ui.library.LibraryTopicWordAdapter`
- ViewModel: `com.nhom18.flashlock.ui.library.LibraryTopicWordsViewModel`

## Lock screen study (Notification-based)
- Service: `com.nhom18.flashlock.service.LockScreenStudyService`
- Receiver: `com.nhom18.flashlock.receiver.LockScreenStudyReceiver`

## Tests
Run:
```
./gradlew :app:testDebugUnitTest
```
