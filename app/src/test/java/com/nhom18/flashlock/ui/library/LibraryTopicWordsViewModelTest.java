package com.nhom18.flashlock.ui.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.Tasks;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.repository.TopicWordRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LibraryTopicWordsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadTopicWords_emitsWords() throws InterruptedException {
        TopicWordRepository repository = topicId -> Tasks.forResult(Arrays.asList(
                buildWord("w1", "Alpha"),
                buildWord("w2", "Beta")
        ));
        LibraryTopicWordsViewModel viewModel = new LibraryTopicWordsViewModel(repository);

        viewModel.loadTopicWords("topic-1");
        List<Word> words = getValue(viewModel.getWords());

        assertNotNull(words);
        assertEquals(2, words.size());
    }

    @Test
    public void loadTopicWords_missingId_setsError() throws InterruptedException {
        TopicWordRepository repository = topicId -> Tasks.forResult(Arrays.asList(buildWord("w1", "Alpha")));
        LibraryTopicWordsViewModel viewModel = new LibraryTopicWordsViewModel(repository);

        viewModel.loadTopicWords(" ");
        String error = getValue(viewModel.getError());

        assertEquals("TOPIC_ID_REQUIRED", error);
    }

    private static Word buildWord(String id, String term) {
        Word word = new Word();
        word.setWordId(id);
        word.setTerm(term);
        return word;
    }

    private static <T> T getValue(LiveData<T> liveData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> data = new AtomicReference<>();
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                data.set(value);
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        return data.get();
    }
}

