package com.nhom18.flashlock.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.databinding.BottomSheetLockScreenTopicsBinding;

import java.util.ArrayList;
import java.util.List;

public class LockScreenTopicPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnTopicsSelectedListener {
        void onSelected(List<String> topicIds);
    }

    private BottomSheetLockScreenTopicsBinding binding;
    private LockScreenTopicAdapter adapter;
    private List<Topic> topics = new ArrayList<>();
    private List<String> selectedIds = new ArrayList<>();
    private OnTopicsSelectedListener listener;

    public static LockScreenTopicPickerBottomSheet newInstance() {
        return new LockScreenTopicPickerBottomSheet();
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics != null ? topics : new ArrayList<>();
    }

    public void setSelectedIds(List<String> selectedIds) {
        this.selectedIds = selectedIds != null ? selectedIds : new ArrayList<>();
    }

    public void setListener(OnTopicsSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetLockScreenTopicsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LockScreenTopicAdapter(topics, selectedIds);
        binding.recyclerTopics.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerTopics.setAdapter(adapter);

        binding.btnSaveTopics.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelected(new ArrayList<>(adapter.getSelectedIds()));
            }
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
