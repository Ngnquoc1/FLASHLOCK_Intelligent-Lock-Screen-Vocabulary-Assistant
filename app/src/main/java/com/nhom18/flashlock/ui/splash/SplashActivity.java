package com.nhom18.flashlock.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startLoadingAnimation();

        // Đợi 3 giây để hiển thị animation, sau đó kiểm tra trạng thái đăng nhập
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthStatus, 3000);
    }

    private void startLoadingAnimation() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.dot_fade);
        Animation anim2 = AnimationUtils.loadAnimation(this, R.anim.dot_fade);
        Animation anim3 = AnimationUtils.loadAnimation(this, R.anim.dot_fade);

        // Tạo độ trễ (offset) cho từng dấu chấm để tạo hiệu ứng làn sóng (wave)
        anim2.setStartOffset(200);
        anim3.setStartOffset(400);

        dot1.startAnimation(anim1);
        dot2.startAnimation(anim2);
        dot3.startAnimation(anim3);
    }

    private void checkAuthStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // Tạm thời vẫn vào MainActivity cho đến khi có LoginActivity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
        finish();
    }
}
