package com.nhom18.flashlock.ui.vocabulary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;
import java.util.ArrayList;
import java.util.List;

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.WordViewHolder> {

    public interface WordActionListener {
        void onDelete(Word word);
        void onEdit(Word word);
    }

    private final List<Word> items = new ArrayList<>();
    private final WordActionListener listener;

    public VocabularyAdapter(WordActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Word> words) {
        items.clear();
        if (words != null) {
            items.addAll(words);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWord;
        private final TextView tvMeaning;
        private final TextView tvStatusTag;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tv_word);
            tvMeaning = itemView.findViewById(R.id.tv_meaning);
            tvStatusTag = itemView.findViewById(R.id.tv_status_tag);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Word word, WordActionListener listener) {
            tvWord.setText(word.getTerm() != null ? word.getTerm() : "");
            tvMeaning.setText(word.getDefinition() != null ? word.getDefinition() : "");

            String status = word.getStatus();
            if (Word.STATUS_MASTERED.equals(status)) {
                tvStatusTag.setText(itemView.getContext().getString(R.string.vocab_status_mastered_tag));
                tvStatusTag.setBackgroundResource(R.drawable.bg_tag_remembered);
            } else if (Word.STATUS_LEARNING.equals(status)) {
                tvStatusTag.setText(itemView.getContext().getString(R.string.vocab_status_learning_tag));
                tvStatusTag.setBackgroundResource(R.drawable.bg_tag_learning);
            } else {
                tvStatusTag.setText(itemView.getContext().getString(R.string.vocab_status_new_tag));
                tvStatusTag.setBackgroundResource(R.drawable.bg_tag_new);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(word);
                }
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(word);
                }
            });
        }
    }
}
