package com.example.buscacelularbackground; // SEU PACOTE AQUI

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Pega a altura da tela do celular para saber até onde o bloco deve cair
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float screenHeight = displayMetrics.heightPixels;

        // 2. Encontra os blocos e o título
        ImageView b1 = findViewById(R.id.block1);
        ImageView b2 = findViewById(R.id.block2);
        ImageView b3 = findViewById(R.id.block3);
        ImageView b4 = findViewById(R.id.block4);
        ImageView b5 = findViewById(R.id.block5);
        TextView title = findViewById(R.id.titleTetris);

        // 3. Inicia as animações de queda
        // (View, Duração em ms, Atraso inicial em ms, Altura da tela)
        animarQueda(b1, 2000, 0, screenHeight);
        animarQueda(b2, 2500, 200, screenHeight);
        animarQueda(b3, 1800, 100, screenHeight);
        animarQueda(b4, 2200, 300, screenHeight);
        animarQueda(b5, 2800, 50, screenHeight);

        // 4. Animação extra: Título pulsando
        ObjectAnimator titleScaleX = ObjectAnimator.ofFloat(title, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator titleScaleY = ObjectAnimator.ofFloat(title, "scaleY", 1f, 1.1f, 1f);
        titleScaleX.setDuration(1000);
        titleScaleY.setDuration(1000);
        titleScaleX.setRepeatCount(ObjectAnimator.INFINITE);
        titleScaleY.setRepeatCount(ObjectAnimator.INFINITE);
        titleScaleX.start();
        titleScaleY.start();

        // 5. Abre o jogo após 3.5 segundos
        new Handler().postDelayed(() -> {
            Intent i = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }, 3500);
    }

    // Função auxiliar para criar a animação de queda
    private void animarQueda(View view, long duration, long delay, float screenHeight) {
        // Cria animação no eixo Y (vertical), de -200 (acima da tela) até altura da tela + 200 (abaixo)
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", -200f, screenHeight + 200f);
        animator.setDuration(duration); // Quanto tempo demora pra cair
        animator.setStartDelay(delay); // Quanto tempo espera pra começar
        animator.setInterpolator(new AccelerateInterpolator()); // Começa devagar e acelera (gravidade)
        animator.start();
    }
}