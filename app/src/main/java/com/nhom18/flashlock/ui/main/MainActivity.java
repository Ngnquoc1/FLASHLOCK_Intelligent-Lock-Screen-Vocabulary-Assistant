package com.nhom18.flashlock.ui.main;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.ui.library.LibraryFragment;
import com.nhom18.flashlock.ui.profile.ProfileFragment;
import com.nhom18.flashlock.ui.vocabulary.VocabularyFragment;

public class MainActivity extends AppCompatActivity {

    private View navHome, navLibrary, navVocabulary, navProfile;
    private ImageView ivTopProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homebatch);

        initViews();
        setupListeners();
        loadTopBarProfile();

        // Mặc định khi vào app sẽ ở tab HOME
        if (savedInstanceState == null) {
            selectTab(R.id.nav_home_item, "HOME");
        }
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home_item);
        navLibrary = findViewById(R.id.nav_library_item);
        navVocabulary = findViewById(R.id.nav_vocabulary_item);
        navProfile = findViewById(R.id.nav_profile_item);
        ivTopProfile = findViewById(R.id.iv_top_profile);
    }

    private void setupListeners() {
        navHome.setOnClickListener(v -> selectTab(R.id.nav_home_item, "HOME"));
        navLibrary.setOnClickListener(v -> selectTab(R.id.nav_library_item, "LIBRARY"));
        navVocabulary.setOnClickListener(v -> selectTab(R.id.nav_vocabulary_item, "VOCABULARY"));
        navProfile.setOnClickListener(v -> selectTab(R.id.nav_profile_item, "PROFILE"));
    }

    private void loadTopBarProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null && ivTopProfile != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .addSnapshotListener((snapshot, e) -> {
                        if (snapshot != null && snapshot.exists()) {
                            String avatarUrl = snapshot.getString("avatarUrl");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_nav_profile)
                                        .into(ivTopProfile);
                            } else {
                                ivTopProfile.setImageResource(R.drawable.ic_nav_profile);
                            }
                        }
                    });
        }
    }

    public void openVocabularyTopics() {
        selectTab(R.id.nav_vocabulary_item, "VOCABULARY", true);
    }

    private void selectTab(int itemId, String title) {
        selectTab(itemId, title, false);
    }

    private void selectTab(int itemId, String title, boolean openTopicsTab) {
        updateNavbarUI(itemId);

        Fragment fragment;
        if (itemId == R.id.nav_profile_item) {
            fragment = ProfileFragment.newInstance();
        } else if (itemId == R.id.nav_vocabulary_item) {
            fragment = VocabularyFragment.newInstance(openTopicsTab);
        } else if (itemId == R.id.nav_library_item) {
            fragment = LibraryFragment.newInstance();
        } else {
            fragment = MainFragment.newInstance(title);
        }

        loadFragment(fragment);
    }

    private void updateNavbarUI(int selectedId) {
        View[] items = {navHome, navLibrary, navVocabulary, navProfile};

        for (View item : items) {
            boolean isSelected = (item.getId() == selectedId);

            if (isSelected) {
                item.setBackgroundResource(R.drawable.bg_nav_icon_selected);
                item.setPadding(0, dpToPx(12), 0, dpToPx(12));
            } else {
                item.setBackground(null);
                item.setPadding(0, 0, 0, 0);
            }

            updateItemChildStyles(item, isSelected);
        }
    }

    private void updateItemChildStyles(View itemView, boolean isSelected) {
        float alpha = isSelected ? 1.0f : 0.6f;
        int color = isSelected ? ContextCompat.getColor(this, R.color.white)
                               : ContextCompat.getColor(this, R.color.on_surface);

        if (itemView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) itemView;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    child.setAlpha(alpha);
                    ((ImageView) child).setColorFilter(color);
                } else if (child instanceof TextView) {
                    child.setAlpha(alpha);
                    ((TextView) child).setTextColor(color);
                }
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
