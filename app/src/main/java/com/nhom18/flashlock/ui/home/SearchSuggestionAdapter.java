package com.nhom18.flashlock.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom18.flashlock.R;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    public static class SearchItem {
        public static final int TYPE_API = 3;

        public String id;
        public String title;
        public String definition;
        public int type;

        public SearchItem(String id, String title, String definition, int type) {
            this.id = id;
            this.title = title;
            this.definition = definition;
            this.type = type;
        }

        public SearchItem(String id, String title, int type) {
            this(id, title, "", type);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(SearchItem item);
        void onAddClick(SearchItem item);
    }

    private List<SearchItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SearchItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchItem item = items.get(position);
        holder.tvText.setText(item.title);

        if (item.definition != null && !item.definition.trim().isEmpty()) {
            holder.tvDef.setText(item.definition);
            holder.tvDef.setVisibility(View.VISIBLE);
        } else {
            holder.tvDef.setVisibility(View.GONE);
        }

        holder.ivIcon.setImageResource(R.drawable.ic_translate);
        holder.ivAdd.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        holder.ivAdd.setOnClickListener(v -> {
            if (listener != null) listener.onAddClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvText;
        TextView tvDef;
        ImageView ivAdd;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_suggestion_icon);
            tvText = itemView.findViewById(R.id.tv_suggestion_text);
            tvDef = itemView.findViewById(R.id.tv_suggestion_def);
            ivAdd = itemView.findViewById(R.id.iv_suggestion_add);
        }
    }
}