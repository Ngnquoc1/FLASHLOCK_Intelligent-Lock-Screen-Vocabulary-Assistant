package com.nhom18.flashlock.ui.vocabulary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Topic;
import java.util.ArrayList;
import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {
    private final List<Topic> items = new ArrayList<>();

    public void submitList(List<Topic> topics) {
        items.clear();
        if (topics != null) {
            items.addAll(topics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvWordCount;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_topic_name);
            tvWordCount = itemView.findViewById(R.id.tv_word_count);
        }

        void bind(Topic topic) {
            tvTitle.setText(topic.getTitle() != null ? topic.getTitle() : "");
            long count = topic.getWordCount();
            tvWordCount.setText(count > 0 ? (count + " WORDS") : "");
        }
    }
}

