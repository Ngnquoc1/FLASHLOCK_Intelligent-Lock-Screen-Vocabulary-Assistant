package com.nhom18.flashlock.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Topic;

import java.util.HashSet;
import java.util.Set;

public class LibraryTopicAdapter extends ListAdapter<Topic, LibraryTopicAdapter.TopicViewHolder> {
    public interface TopicActionListener {
        void onStudyNow(Topic topic);
        void onSaveTopic(Topic topic);
        void onOpenTopic(Topic topic);
    }

    private final TopicActionListener actionListener;
    private Set<String> savedTopicIds = new HashSet<>();

    private static final DiffUtil.ItemCallback<Topic> DIFF_CALLBACK = new DiffUtil.ItemCallback<Topic>() {
        @Override
        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            String oldDocId = oldItem.getDocumentId();
            String newDocId = newItem.getDocumentId();
            if (oldDocId != null && newDocId != null) {
                return oldDocId.equals(newDocId);
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            if (oldItem == newItem) {
                return true;
            }
            return safeEquals(oldItem.getTitle(), newItem.getTitle())
                    && safeEquals(oldItem.getCategory(), newItem.getCategory())
                    && safeEquals(oldItem.getThumbnailUrl(), newItem.getThumbnailUrl())
                    && safeEquals(oldItem.getLanguage(), newItem.getLanguage())
                    && oldItem.getWordCount() == newItem.getWordCount();
        }
    };

    public LibraryTopicAdapter(TopicActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    public void setSavedTopicIds(Set<String> ids) {
        savedTopicIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_library_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        holder.bind(getItem(position), actionListener);
    }

    class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvWordCount;
        private final TextView tvLanguageBadge;
        private final ImageView ivCover;
        private final View btnStudyNow;
        private final ImageButton btnSave;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_topic_name);
            tvWordCount = itemView.findViewById(R.id.tv_word_count);
            tvLanguageBadge = itemView.findViewById(R.id.tv_premium_badge);
            ivCover = itemView.findViewById(R.id.iv_topic_cover);
            btnStudyNow = itemView.findViewById(R.id.btn_study_now);
            btnSave = itemView.findViewById(R.id.btn_action);
        }

        void bind(Topic topic, TopicActionListener actionListener) {
            tvTitle.setText(topic.getTitle() != null ? topic.getTitle() : "");
            long count = topic.getWordCount();
            if (count > 0) {
                tvWordCount.setText(itemView.getContext().getString(R.string.topic_word_count_format, count));
            } else {
                tvWordCount.setText("");
            }

            Glide.with(itemView)
                    .load(topic.getThumbnailUrl())
                    .placeholder(R.drawable.pic_galaxy)
                    .error(R.drawable.pic_galaxy)
                    .into(ivCover);

            String language = topic.getLanguage();
            if (language == null || language.trim().isEmpty()) {
                tvLanguageBadge.setVisibility(View.GONE);
            } else {
                tvLanguageBadge.setVisibility(View.VISIBLE);
                String label = language.equalsIgnoreCase("vi") ? "VI" : "EN";
                tvLanguageBadge.setText(label);
            }

            boolean isSaved = topic.getTopicId() != null && savedTopicIds.contains(topic.getTopicId());
            btnSave.setEnabled(!isSaved);
            btnSave.setAlpha(isSaved ? 0.7f : 1.0f);
            if (isSaved) {
                btnSave.setImageResource(R.drawable.ic_check_circle_small);
                btnSave.setContentDescription(itemView.getContext().getString(R.string.library_action_saved));
            } else {
                btnSave.setImageResource(R.drawable.ic_sync);
                btnSave.setContentDescription(itemView.getContext().getString(R.string.library_action_save));
            }

            btnStudyNow.setOnClickListener(v -> actionListener.onStudyNow(topic));
            btnSave.setOnClickListener(v -> {
                if (!isSaved) {
                    actionListener.onSaveTopic(topic);
                }
            });
            itemView.setOnClickListener(v -> actionListener.onOpenTopic(topic));
        }
    }

    private static boolean safeEquals(String first, String second) {
        if (first == null) {
            return second == null;
        }
        return first.equals(second);
    }
}