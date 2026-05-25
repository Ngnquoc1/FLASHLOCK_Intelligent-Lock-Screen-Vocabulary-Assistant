package com.nhom18.flashlock.ui.vocabulary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.Timestamp;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.TopicProgress;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import com.nhom18.flashlock.data.remote.FirebaseWordDataSource;
import com.nhom18.flashlock.data.repository.FirebaseSavedTopicRepository;
import com.nhom18.flashlock.data.repository.FirebaseWordRepository;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;
import com.nhom18.flashlock.data.repository.TopicProgressRepository;
import com.nhom18.flashlock.data.remote.FirebaseTopicProgressDataSource;
import com.nhom18.flashlock.data.repository.FirebaseTopicProgressRepository;
import com.nhom18.flashlock.data.repository.WordRepository;
import java.util.ArrayList;
import java.util.List;

public class VocabularyViewModel extends ViewModel {
    private final WordRepository wordRepository;
    private final SavedTopicRepository topicRepository;
    private final TopicProgressRepository topicProgressRepository;

    private final MutableLiveData<List<Word>> words = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Topic>> topics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TopicProgress>> topicProgress = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    public VocabularyViewModel() {
        this(new FirebaseWordRepository(new FirebaseWordDataSource()),
                new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource()),
                new FirebaseTopicProgressRepository(new FirebaseTopicProgressDataSource()));
    }

    VocabularyViewModel(WordRepository wordRepository, SavedTopicRepository topicRepository) {
        this(wordRepository, topicRepository, new FirebaseTopicProgressRepository(new FirebaseTopicProgressDataSource()));
    }

    VocabularyViewModel(WordRepository wordRepository,
                        SavedTopicRepository topicRepository,
                        TopicProgressRepository topicProgressRepository) {
        this.wordRepository = wordRepository;
        this.topicRepository = topicRepository;
        this.topicProgressRepository = topicProgressRepository;
    }

    public LiveData<List<Word>> getWords() {
        return words;
    }

    public LiveData<List<Topic>> getTopics() {
        return topics;
    }

    public LiveData<List<TopicProgress>> getTopicProgress() {
        return topicProgress;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadVocabulary() {
        loading.setValue(true);
        wordRepository.getAllWords().addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                words.postValue(task.getResult());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "LOAD_WORDS_FAILED");
            }
        });
    }

    public void searchVocabulary(String query) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            loadVocabulary();
            return;
        }
        loading.setValue(true);
        wordRepository.searchWordsByTerm(term).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                words.postValue(task.getResult());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "SEARCH_WORDS_FAILED");
            }
        });
    }

    public void loadTopics() {
        loading.setValue(true);
        topicRepository.getSavedTopics().addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                topics.postValue(task.getResult());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "LOAD_TOPICS_FAILED");
            }
        });
    }

    public void saveTopic(Topic topic) {
        if (topic == null || topic.getTopicId() == null || topic.getTopicId().trim().isEmpty()) {
            error.setValue("TOPIC_ID_REQUIRED");
            return;
        }
        loading.setValue(true);
        topicRepository.saveTopic(topic).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                loadTopics();
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "SAVE_TOPIC_FAILED");
            }
        });
    }

    public void loadTopicProgress() {
        topicProgressRepository.getProgress().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                topicProgress.postValue(task.getResult());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "LOAD_TOPIC_PROGRESS_FAILED");
            }
        });
    }

    public void saveTopicProgress(TopicProgress progress) {
        if (progress == null || progress.getTopicId() == null || progress.getTopicId().trim().isEmpty()) {
            error.setValue("TOPIC_ID_REQUIRED");
            return;
        }
        topicProgressRepository.saveProgress(progress).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "SAVE_TOPIC_PROGRESS_FAILED");
            }
        });
    }

    public void addWord(String term, String definition) {
        String termValue = term == null ? "" : term.trim();
        String definitionValue = definition == null ? "" : definition.trim();
        if (termValue.isEmpty()) {
            error.setValue("WORD_TERM_REQUIRED");
            return;
        }
        if (definitionValue.isEmpty()) {
            error.setValue("WORD_DEFINITION_REQUIRED");
            return;
        }

        Word word = new Word();
        word.setTerm(termValue);
        word.setDefinition(definitionValue);
        word.setStatus(Word.STATUS_NEW);
        word.setNextReviewAt(Timestamp.now());

        loading.setValue(true);
        wordRepository.addWord(word).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                loadVocabulary();
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "ADD_WORD_FAILED");
            }
        });
    }

    public void addWord(Word word) {
        if (word == null) {
            error.setValue("WORD_REQUIRED");
            return;
        }
        String termValue = word.getTerm() == null ? "" : word.getTerm().trim();
        String definitionValue = word.getDefinition() == null ? "" : word.getDefinition().trim();
        if (termValue.isEmpty()) {
            error.setValue("WORD_TERM_REQUIRED");
            return;
        }
        if (definitionValue.isEmpty()) {
            error.setValue("WORD_DEFINITION_REQUIRED");
            return;
        }
        word.setTerm(termValue);
        word.setDefinition(definitionValue);
        if (word.getStatus() == null || word.getStatus().trim().isEmpty()) {
            word.setStatus(Word.STATUS_NEW);
        }
        if (word.getNextReviewAt() == null) {
            word.setNextReviewAt(Timestamp.now());
        }

        loading.setValue(true);
        wordRepository.addWord(word).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                loadVocabulary();
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "ADD_WORD_FAILED");
            }
        });
    }

    public void updateWord(Word word) {
        if (word == null || word.getWordId() == null || word.getWordId().trim().isEmpty()) {
            error.setValue("WORD_ID_REQUIRED");
            return;
        }
        String termValue = word.getTerm() == null ? "" : word.getTerm().trim();
        String definitionValue = word.getDefinition() == null ? "" : word.getDefinition().trim();
        if (termValue.isEmpty()) {
            error.setValue("WORD_TERM_REQUIRED");
            return;
        }
        if (definitionValue.isEmpty()) {
            error.setValue("WORD_DEFINITION_REQUIRED");
            return;
        }
        word.setTerm(termValue);
        word.setDefinition(definitionValue);

        loading.setValue(true);
        wordRepository.updateWord(word).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                loadVocabulary();
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "UPDATE_WORD_FAILED");
            }
        });
    }

    public void deleteWord(String wordId) {
        if (wordId == null || wordId.trim().isEmpty()) {
            error.setValue("WORD_ID_REQUIRED");
            return;
        }
        loading.setValue(true);
        wordRepository.deleteWord(wordId).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                loadVocabulary();
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "DELETE_WORD_FAILED");
            }
        });
    }
}
