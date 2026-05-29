package com.nhom18.flashlock.ui.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.databinding.ItemLockScreenTopicBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockScreenTopicAdapter extends RecyclerView.Adapter<LockScreenTopicAdapter.TopicViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<Topic> topics;
    private final Set<String> selectedIds;
    private OnSelectionChangedListener selectionChangedListener;

    public LockScreenTopicAdapter(List<Topic> topics, List<String> selectedIds) {
        this.topics = topics;
        this.selectedIds = new HashSet<>(selectedIds);
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLockScreenTopicBinding binding = ItemLockScreenTopicBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TopicViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topics.get(position);
        String topicId = topic.getTopicId();
        boolean isSelected = topicId != null && selectedIds.contains(topicId);

        holder.binding.checkboxTopic.setOnCheckedChangeListener(null);
        holder.binding.checkboxTopic.setText(topic.getTitle() != null ? topic.getTitle() : "");
        holder.binding.checkboxTopic.setChecked(isSelected);
        holder.binding.checkboxTopic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (topicId == null) {
                return;
            }
            if (isChecked) {
                selectedIds.add(topicId);
            } else {
                selectedIds.remove(topicId);
            }
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(selectedIds.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return topics != null ? topics.size() : 0;
    }

    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final ItemLockScreenTopicBinding binding;

        TopicViewHolder(ItemLockScreenTopicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}