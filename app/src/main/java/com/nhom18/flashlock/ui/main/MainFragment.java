package com.nhom18.flashlock.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.nhom18.flashlock.R;

public class MainFragment extends Fragment {
    private static final String ARG_TITLE = "arg_title";

    public static MainFragment newInstance(String title) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            TextView textView = view.findViewById(R.id.fragment_title);
            textView.setText(title);
        }
        return view;
    }
}
