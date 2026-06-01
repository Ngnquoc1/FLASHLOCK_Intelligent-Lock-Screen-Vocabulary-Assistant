package com.nhom18.flashlock.ui.home;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.dictionary.DictionaryResponse;
import com.nhom18.flashlock.data.repository.DictionaryRepository;
import com.nhom18.flashlock.databinding.BottomSheetDictionaryDetailBinding;

import java.util.Locale;

public class DictionaryDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_WORD_QUERY = "arg_word_query";

    private BottomSheetDictionaryDetailBinding binding;
    private HomeDashboardViewModel viewModel;
    private DictionaryRepository repository;

    private DictionaryResponse data;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    public static DictionaryDetailBottomSheet newInstance(String wordQuery) {
        DictionaryDetailBottomSheet fragment = new DictionaryDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_WORD_QUERY, wordQuery);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetDictionaryDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeDashboardViewModel.class);
        repository = new DictionaryRepository();

        setupTts();

        if (getArguments() != null) {
            String wordQuery = getArguments().getString(ARG_WORD_QUERY);
            if (wordQuery != null) {
                binding.tvWord.setText(wordQuery);
                binding.btnAddToMyWords.setEnabled(false);
                fetchDictionaryData(wordQuery);
            }
        }

        binding.btnSpeak.setOnClickListener(v -> speak());
        binding.btnAddToMyWords.setOnClickListener(v -> addToMyWords());
    }

    private void fetchDictionaryData(String wordQuery) {
        repository.searchOnline(wordQuery, new DictionaryRepository.DictionaryCallback() {
            @Override
            public void onSuccess(DictionaryResponse result) {
                if (!isAdded() || binding == null) return;

                data = result;
                displayData();
                binding.btnAddToMyWords.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || binding == null) return;
                Toast.makeText(getContext(), "Không tìm thấy chi tiết từ này!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTts() {
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    private void displayData() {
        binding.tvWord.setText(data.word);

        if (data.phonetic != null && !data.phonetic.isEmpty()) {
            binding.tvPhonetic.setText(data.phonetic);
            binding.tvPhonetic.setVisibility(View.VISIBLE);
        } else {
            binding.tvPhonetic.setVisibility(View.GONE);
        }

        binding.llMeaningsContainer.removeAllViews();

        if (data.meanings != null) {
            for (DictionaryResponse.Meaning meaning : data.meanings) {
                View meaningView = getLayoutInflater().inflate(R.layout.item_dictionary_meaning, binding.llMeaningsContainer, false);

                TextView tvPartOfSpeech = meaningView.findViewById(R.id.tv_part_of_speech);
                LinearLayout llDefinitions = meaningView.findViewById(R.id.ll_definitions_container);

                tvPartOfSpeech.setText(meaning.partOfSpeech);

                if (meaning.definitions != null) {
                    for (int i = 0; i < meaning.definitions.size(); i++) {
                        DictionaryResponse.Definition def = meaning.definitions.get(i);

                        View defView = getLayoutInflater().inflate(R.layout.item_dictionary_definition, llDefinitions, false);
                        TextView tvIndex = defView.findViewById(R.id.tv_definition_index);
                        TextView tvText = defView.findViewById(R.id.tv_definition_text);
                        TextView tvExample = defView.findViewById(R.id.tv_example_text);

                        tvIndex.setText((i + 1) + ".");
                        tvText.setText(def.definition);

                        if (def.example != null && !def.example.isEmpty()) {
                            tvExample.setText("\"" + def.example + "\"");
                            tvExample.setVisibility(View.VISIBLE);
                        } else {
                            tvExample.setVisibility(View.GONE);
                        }

                        llDefinitions.addView(defView);
                    }
                }
                binding.llMeaningsContainer.addView(meaningView);
            }
        }
    }

    private void speak() {
        if (!ttsReady || data == null) return;
        tts.speak(data.word, TextToSpeech.QUEUE_FLUSH, null, "dictionary_detail");
    }

    private void addToMyWords() {
        if (data == null) return;

        Word word = new Word();
        word.setTerm(data.word);

        if (data.meanings != null && !data.meanings.isEmpty()
                && data.meanings.get(0).definitions != null && !data.meanings.get(0).definitions.isEmpty()) {
            word.setDefinition(data.meanings.get(0).definitions.get(0).definition);
            word.setWordType(data.meanings.get(0).partOfSpeech);
            word.setExample(data.meanings.get(0).definitions.get(0).example);
        }

        if (data.phonetic != null) {
            word.setPronunciation(data.phonetic);
        }

        word.setStatus(Word.STATUS_NEW);
        word.setNextReviewAt(com.google.firebase.Timestamp.now());

        viewModel.addCustomWordToMyWords(word);

        Toast.makeText(getContext(), "Added to My Words", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}