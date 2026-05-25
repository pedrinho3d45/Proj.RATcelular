package com.example.buscacelularbackground;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.textoInfo);

        // 1. Permissões
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 2. Serviço
        Intent intentServico = new Intent(this, ServicoMonitoramento.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServico);
        } else {
            startService(intentServico);
        }

        // 3. Cria o atalho
        criarAtalhoNaTelaInicial();

        // 4. AGENDAMENTO PARA ESCONDER O ORIGINAL
        // Espera 5 segundos para garantir que o atalho foi criado, depois esconde o original
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                esconderIconeDaLista();
            }
        }, 5000); // 5000 milissegundos = 5 segundos
    }

    private void criarAtalhoNaTelaInicial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {

                // Verifica se o atalho já existe para não criar duplicado
                // (Isso evita spam de ícones na tela)
                boolean jaExiste = false;
                for (ShortcutInfo s : shortcutManager.getPinnedShortcuts()) {
                    if (s.getId().equals("id-alarme-espelho")) {
                        jaExiste = true;
                        break;
                    }
                }

                if (!jaExiste) {
                    Intent intentAlvo = new Intent(this, MainActivity.class);
                    intentAlvo.setAction(Intent.ACTION_MAIN);

                    ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, "id-alarme-espelho")
                            .setShortLabel("Alarme Espelho")
                            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                            .setIntent(intentAlvo)
                            .build();

                    shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                }
            }
        }
    }

    private void esconderIconeDaLista() {
        try {
            PackageManager p = getPackageManager();
            ComponentName componentName = new ComponentName(this, MainActivity.class);

            // Verifica qual o estado atual
            int estadoAtual = p.getComponentEnabledSetting(componentName);

            // Só desativa se ainda estiver ativado
            if (estadoAtual != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {

                p.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}