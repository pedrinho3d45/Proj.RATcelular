package com.example.buscacelularbackground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ServicoMonitoramento extends Service {

    Handler handler = new Handler();
    AudioManager audioManager = null;
    private MediaPlayer mediaPlayer;
    private DatabaseReference meuBanco;

    // Variável para guardar o recorde
    private int ultimoHighScore = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        // Conecta ao Firebase ("status_alarme")
        meuBanco = FirebaseDatabase.getInstance().getReference("status_alarme");

        // Escuta mudanças na nuvem
        meuBanco.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String valor = snapshot.getValue(String.class);
                if (valor != null && "LIGAR".equals(valor.trim())) { // Trim evita erros de espaço
                    tocarAlarme();
                } else {
                    pararAlarme();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Verifica se chegou um novo score vindo do jogo
        if (intent != null && intent.hasExtra("NOVO_SCORE")) {
            ultimoHighScore = intent.getIntExtra("NOVO_SCORE", 0);
        }

        // Atualiza a notificação (com score novo ou antigo)
        criarNotificacaoPermanente();

        // START_STICKY garante que o serviço renasça se for morto
        return START_STICKY;
    }

    private void criarNotificacaoPermanente() {
        String channelId = "canal_busca_celular";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Status do Tetris",
                    NotificationManager.IMPORTANCE_LOW // Low = Sem som/vibração (discreto)
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // Define o texto dinamicamente
        String textoNotificacao;
        if (ultimoHighScore > 0) {
            textoNotificacao = "🏆 Recorde Atual: " + ultimoHighScore;
        } else {
            textoNotificacao = "☁️ Salvando progresso...";
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Tetris Cloud")
                .setContentText(textoNotificacao) // Mostra o score!
                .setSmallIcon(R.drawable.icone_tetris_transparente) // Sua logo sem fundo
                .setOngoing(false) // Permite arrastar (menos suspeito)
                .setOnlyAlertOnce(true) // Não vibra toda vez que atualiza o score

                // Abre o app ao clicar
                .setContentIntent(android.app.PendingIntent.getActivity(
                        this, 0, new Intent(this, MainActivity.class), android.app.PendingIntent.FLAG_IMMUTABLE
                ))
                .build();

        startForeground(1, notification);
    }

    // --- LÓGICA DO ALARME (HaramAlarme) ---
    Runnable tarefaRepetitiva = new Runnable() {
        @Override
        public void run() {
            int volumeMaximo = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeMaximo, 0);
            handler.postDelayed(this, 500); // Força volume máximo a cada 0.5s
        }
    };

    private void aumentarVolume() {
        handler.removeCallbacks(tarefaRepetitiva);
        handler.post(tarefaRepetitiva);
    }

    private void tocarAlarme() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        try {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) aumentarVolume();

            Uri som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (som == null) som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, som);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void pararAlarme() {
        if (mediaPlayer != null) {
            handler.removeCallbacks(tarefaRepetitiva);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}