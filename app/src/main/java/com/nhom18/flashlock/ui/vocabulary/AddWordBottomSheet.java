package com.nhom18.flashlock.ui.vocabulary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;

public class AddWordBottomSheet extends BottomSheetDialogFragment {

    public enum Mode { ADD, EDIT }

    public interface Listener {
        void onSubmit(AddWordInput input);
    }

    public static class AddWordInput {
        private final String wordId;
        private final String term;
        private final String pronunciation;
        private final String wordType;
        private final String definition;
        private final String example;
        private final String status;

        public AddWordInput(String wordId, String term, String pronunciation, String wordType, String definition, String example, String status) {
            this.wordId = wordId;
            this.term = term;
            this.pronunciation = pronunciation;
            this.wordType = wordType;
            this.definition = definition;
            this.example = example;
            this.status = status;
        }

        public String getWordId() { return wordId; }
        public String getTerm() { return term; }
        public String getPronunciation() { return pronunciation; }
        public String getWordType() { return wordType; }
        public String getDefinition() { return definition; }
        public String getExample() { return example; }
        public String getStatus() { return status; }
    }

    private Listener listener;
    private Mode mode = Mode.ADD;
    private Word editingWord;

    public void setListener(Listener listener) { this.listener = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_word, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_subtitle);
        TextView tvWordChip = view.findViewById(R.id.tv_word_chip);
        TextInputLayout tilTerm = view.findViewById(R.id.til_term);
        TextInputLayout tilDefinition = view.findViewById(R.id.til_definition);
        TextInputLayout tilStatus = view.findViewById(R.id.til_status);
        TextInputEditText etTerm = view.findViewById(R.id.et_term);
        TextInputEditText etPronunciation = view.findViewById(R.id.et_pronunciation);
        TextInputEditText etWordType = view.findViewById(R.id.et_word_type);
        TextInputEditText etDefinition = view.findViewById(R.id.et_definition);
        TextInputEditText etExample = view.findViewById(R.id.et_example);
        AutoCompleteTextView actStatus = view.findViewById(R.id.act_status);
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        ImageButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnSubmit = view.findViewById(R.id.btn_submit);

        // 1. THIẾT LẬP DROPDOWN (Frontend logic)
        String[] statuses = new String[]{
                getString(R.string.vocab_status_new),
                getString(R.string.vocab_status_learning),
                getString(R.string.vocab_status_mastered)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, statuses);
        actStatus.setAdapter(adapter);
        
        // Quan trọng: Để AutoCompleteTextView luôn xổ xuống khi click
        actStatus.setOnClickListener(v -> actStatus.showDropDown());

        // 2. LOGIC CHO CHẾ ĐỘ SỬA
        if (mode == Mode.EDIT && editingWord != null) {
            tvTitle.setText(R.string.vocab_sheet_edit_title);
            tvSubtitle.setText(R.string.vocab_sheet_edit_subtitle);
            btnSubmit.setText(R.string.vocab_btn_save);
            tvWordChip.setVisibility(View.VISIBLE);
            tvWordChip.setText(editingWord.getTerm());
            etTerm.setText(editingWord.getTerm());
            etPronunciation.setText(editingWord.getPronunciation());
            etWordType.setText(editingWord.getWordType());
            etDefinition.setText(editingWord.getDefinition());
            etExample.setText(editingWord.getExample());
            actStatus.setText(statusLabelFromCode(editingWord.getStatus()), false);
        } else {
            actStatus.setText(statuses[0], false); // Mặc định là Từ mới
        }

        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        // 3. BẮT LỖI BỎ TRỐNG (Frontend Validation)
        btnSubmit.setOnClickListener(v -> {
            String term = getTrimmed(etTerm);
            String definition = getTrimmed(etDefinition);
            String statusLabel = actStatus.getText() != null ? actStatus.getText().toString().trim() : "";

            tilTerm.setError(null);
            tilDefinition.setError(null);
            tilStatus.setError(null);

            boolean valid = true;
            if (term.isEmpty()) {
                tilTerm.setError(getString(R.string.vocab_error_term_required));
                valid = false;
            }
            if (definition.isEmpty()) {
                tilDefinition.setError(getString(R.string.vocab_error_definition_required));
                valid = false;
            }
            if (statusLabel.isEmpty()) {
                tilStatus.setError(getString(R.string.vocab_error_status_required));
                valid = false;
            }

            if (!valid) return;

            if (listener != null) {
                String statusCode = statusCodeFromLabel(statusLabel);
                AddWordInput input = new AddWordInput(
                        editingWord != null ? editingWord.getWordId() : null,
                        term, getTrimmed(etPronunciation), getTrimmed(etWordType),
                        definition, getTrimmed(etExample), statusCode
                );
                listener.onSubmit(input);
            }
            dismiss();
        });
    }

    // Các hàm phụ trợ
    public void setEditingWord(Word word) {
        this.editingWord = word;
        this.mode = word != null ? Mode.EDIT : Mode.ADD;
    }

    private String getTrimmed(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String statusLabelFromCode(String code) {
        if (Word.STATUS_MASTERED.equals(code)) return getString(R.string.vocab_status_mastered);
        if (Word.STATUS_LEARNING.equals(code)) return getString(R.string.vocab_status_learning);
        return getString(R.string.vocab_status_new);
    }

    private String statusCodeFromLabel(String label) {
        if (getString(R.string.vocab_status_mastered).equals(label)) return Word.STATUS_MASTERED;
        if (getString(R.string.vocab_status_learning).equals(label)) return Word.STATUS_LEARNING;
        return Word.STATUS_NEW;
    }
}
