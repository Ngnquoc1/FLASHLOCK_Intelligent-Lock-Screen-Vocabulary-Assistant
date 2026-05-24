package com.nhom18.flashlock.ui.vocabulary;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.model.TopicProgress;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;
import com.nhom18.flashlock.data.repository.WordRepository;
import com.nhom18.flashlock.data.repository.TopicProgressRepository;
import com.nhom18.flashlock.util.LiveDataTestUtil;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class VocabularyViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void addWord_missingTerm_setsError() throws InterruptedException {
        FakeWordRepository wordRepository = new FakeWordRepository();
        VocabularyViewModel viewModel = new VocabularyViewModel(wordRepository, new FakeSavedTopicRepository(), new FakeTopicProgressRepository());

        Word word = new Word();
        word.setTerm(" ");
        word.setDefinition("definition");

        viewModel.addWord(word);

        String error = LiveDataTestUtil.getOrAwaitValue(viewModel.getError(), 1000);
        Assert.assertEquals("WORD_TERM_REQUIRED", error);
        Assert.assertFalse(wordRepository.addCalled);
    }

    @Test
    public void deleteWord_missingId_setsError() throws InterruptedException {
        VocabularyViewModel viewModel = new VocabularyViewModel(new FakeWordRepository(), new FakeSavedTopicRepository(), new FakeTopicProgressRepository());

        viewModel.deleteWord(" ");

        String error = LiveDataTestUtil.getOrAwaitValue(viewModel.getError(), 1000);
        Assert.assertEquals("WORD_ID_REQUIRED", error);
    }

    private static class FakeWordRepository implements WordRepository {
        boolean addCalled = false;
        boolean updateCalled = false;
        boolean deleteCalled = false;

        @Override
        public Task<String> addWord(Word word) {
            addCalled = true;
            return Tasks.forResult("id");
        }

        @Override
        public Task<Void> updateWord(Word word) {
            updateCalled = true;
            return Tasks.forResult(null);
        }

        @Override
        public Task<Void> deleteWord(String wordId) {
            deleteCalled = true;
            return Tasks.forResult(null);
        }

        @Override
        public Task<List<Word>> getAllWords() {
            return Tasks.forResult(new ArrayList<>());
        }

        @Override
        public Task<List<Word>> getDueWords(Timestamp currentTime) {
            return Tasks.forResult(new ArrayList<>());
        }

        @Override
        public Task<List<Word>> searchWordsByTerm(String query) {
            return Tasks.forResult(new ArrayList<>());
        }

        @Override
        public ListenerRegistration observeAllWords(WordListListener listener) {
            return null;
        }

        @Override
        public ListenerRegistration observeDueWords(Timestamp currentTime, WordListListener listener) {
            return null;
        }
    }

    private static class FakeSavedTopicRepository implements SavedTopicRepository {
        @Override
        public Task<List<Topic>> getSavedTopics() {
            return Tasks.forResult(new ArrayList<>());
        }

        @Override
        public Task<Void> saveTopic(Topic topic) {
            return Tasks.forResult(null);
        }
    }

    private static class FakeTopicProgressRepository implements TopicProgressRepository {
        @Override
        public Task<List<TopicProgress>> getProgress() {
            return Tasks.forResult(new ArrayList<>());
        }

        @Override
        public Task<Void> saveProgress(TopicProgress progress) {
            return Tasks.forResult(null);
        }
    }
}
