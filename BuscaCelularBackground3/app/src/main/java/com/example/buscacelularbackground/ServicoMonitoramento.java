package com.example.buscacelularbackground; // MANTENHA O SEU PACOTE ORIGINAL AQUI SE FOR DIFERENTE

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Handler;



public class ServicoMonitoramento extends Service {

    Handler handler = new Handler();
    AudioManager audioManager = null;

    private MediaPlayer mediaPlayer;
    private DatabaseReference meuBanco;

    @Override
    public void onCreate() {
        super.onCreate();
        // Conecta ao Firebase assim que o serviço nasce
        meuBanco = FirebaseDatabase.getInstance().getReference("status_alarme");

        // Começa a escutar as mudanças
        meuBanco.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String valor = snapshot.getValue(String.class);
                if ("LIGAR".equals(valor)) {
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
        // Cria a notificação obrigatória para rodar em segundo plano
        criarNotificacaoPermanente();

        // Se o Android matar o app por falta de memória, ele tenta reiniciar (START_STICKY)
        return START_STICKY;
    }

    private void criarNotificacaoPermanente() {
        String channelId = "canal_busca_celular";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Monitoramento de Alarme",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Correio de Voz")
                .setContentText("Você tem novos correios de voz")
                .setSmallIcon(R.drawable.haramvoice)
                .build();

        // Transforma este serviço em Foreground (Prioridade Alta)
        startForeground(1, notification);
    }


    Runnable tarefaRepetitiva = new Runnable() {
        @Override
        public void run() {
            // Descobre o volume máximo possível para o stream de ALARME
            int volumeMaximo = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

            // Define o volume do stream de ALARME para o máximo encontrado
            // A flag 0 evita que apareça aquela barrinha de volume na tela (UI)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeMaximo, 0);

            // Agenda para rodar a si mesmo novamente em 1 segundo (1000ms)
            handler.postDelayed(this, 500);
        }
    };


    private void aumentarVolume() {
        // 1. Configurar o AudioManager para aumentar o volume do sistema

        // Garante que não tenha duplicatas rodando
        handler.removeCallbacks(tarefaRepetitiva);
        // Começa agora
        handler.post(tarefaRepetitiva);
    }


    private void tocarAlarme() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        try {

            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

            if (audioManager != null) {
                aumentarVolume();
            }

            Uri som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (som == null) som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            // Instancia o MediaPlayer manualmente para configurar os atributos antes
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, som);

            // 2. Definir que este áudio é um ALARME (importante para "furar" modos silenciosos simples)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                // Para versões antigas do Android
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare(); // Necessário quando usamos o construtor new MediaPlayer()
            mediaPlayer.start();



        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public IBinder onBind(Intent intent) {
        return null;
    }
}