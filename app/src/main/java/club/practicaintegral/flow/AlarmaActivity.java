package club.practicaintegral.flow;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pantalla de la alarma: suena hasta que la paras (o 2 minutos). */
public class AlarmaActivity extends Activity {
    Ringtone tono;
    Vibrator vib;
    final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String titulo = getIntent().getStringExtra("titulo");
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(56, 56, 56, 56);
        l.setBackgroundColor(Color.parseColor("#EFEAE2"));
        TextView t1 = new TextView(this);
        t1.setText("Amable Flow · tu acción comprometida");
        t1.setTextSize(16); t1.setTextColor(Color.parseColor("#87634E")); t1.setGravity(Gravity.CENTER);
        TextView t2 = new TextView(this);
        t2.setText(titulo == null ? "Es la hora de tu acción" : titulo);
        t2.setTextSize(26); t2.setTextColor(Color.parseColor("#3E2C20")); t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 32, 0, 56);
        Button parar = new Button(this);
        parar.setText("Detener la alarma");
        parar.setOnClickListener(v -> parar(false));
        Button abrir = new Button(this);
        abrir.setText("Abrir Amable Flow");
        abrir.setOnClickListener(v -> parar(true));
        l.addView(t1); l.addView(t2); l.addView(parar); l.addView(abrir);
        setContentView(l);

        Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        tono = RingtoneManager.getRingtone(this, u);
        if (tono != null) {
            tono.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            if (Build.VERSION.SDK_INT >= 28) tono.setLooping(true);
            tono.play();
        }
        vib = getSystemService(Vibrator.class);
        if (vib != null) vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 800, 800}, 0));
        h.postDelayed(() -> parar(false), 120000);
    }

    void parar(boolean abrirApp) {
        h.removeCallbacksAndMessages(null);
        if (tono != null) tono.stop();
        if (vib != null) vib.cancel();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(AlarmaReceiver.AVISO);
        if (abrirApp) startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (tono != null) tono.stop();
        if (vib != null) vib.cancel();
        super.onDestroy();
    }
}
