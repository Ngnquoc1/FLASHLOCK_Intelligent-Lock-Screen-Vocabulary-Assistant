package com.nhom18.flashlock.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;

public class LibraryTopicWordAdapter extends ListAdapter<Word, LibraryTopicWordAdapter.WordViewHolder> {

    private static final DiffUtil.ItemCallback<Word> DIFF_CALLBACK = new DiffUtil.ItemCallback<Word>() {
        @Override
        public boolean areItemsTheSame(@NonNull Word oldItem, @NonNull Word newItem) {
            String oldId = oldItem.getWordId();
            String newId = newItem.getWordId();
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Word oldItem, @NonNull Word newItem) {
            if (oldItem == newItem) {
                return true;
            }
            return safeEquals(oldItem.getTerm(), newItem.getTerm())
                    && safeEquals(oldItem.getDefinition(), newItem.getDefinition())
                    && safeEquals(oldItem.getWordType(), newItem.getWordType());
        }
    };

    public LibraryTopicWordAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTerm;
        private final TextView tvDefinition;
        private final TextView tvWordType;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTerm = itemView.findViewById(R.id.tv_word_term);
            tvDefinition = itemView.findViewById(R.id.tv_word_definition);
            tvWordType = itemView.findViewById(R.id.tv_word_type);
        }

        void bind(Word word) {
            tvTerm.setText(word.getTerm() != null ? word.getTerm() : "");
            tvDefinition.setText(word.getDefinition() != null ? word.getDefinition() : "");

            String wordType = word.getWordType();
            if (wordType == null || wordType.trim().isEmpty()) {
                tvWordType.setVisibility(View.GONE);
            } else {
                tvWordType.setVisibility(View.VISIBLE);
                tvWordType.setText(wordType);
            }
        }
    }

    private static boolean safeEquals(String first, String second) {
        if (first == null) {
            return second == null;
        }
        return first.equals(second);
    }
}

