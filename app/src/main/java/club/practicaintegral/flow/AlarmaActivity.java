package club.practicaintegral.flow;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Pantalla de la alarma (1.3, Gustavo: «bonita, como la de Amable Life»): fondo con degradado, la chica meditando
 * con su pluma (respira y la pluma flota), la acción, y Enterado · Posponer (5, 15, 30 min, 1, 2, 3 h) · Abrir Amable Flow.
 * Suena hasta que la paras (o 2 minutos).
 */
public class AlarmaActivity extends Activity {
    static final int[] MINUTOS = {5, 15, 30, 60, 120, 180};
    Ringtone tono;
    Vibrator vib;
    final Handler h = new Handler(Looper.getMainLooper());

    int dp(float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    TextView texto(String t, float sp, int color, boolean negrita) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.CENTER);
        if (negrita) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    Button boton(String t, int fondo, int letra, int borde) {
        Button b = new Button(this);
        b.setText(t); b.setAllCaps(false); b.setTextSize(19); b.setTextColor(letra); b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable(); g.setColor(fondo); g.setCornerRadius(dp(18));
        if (borde != 0) g.setStroke(dp(1.5f), borde);
        b.setBackground(g); b.setPadding(dp(12), dp(15), dp(12), dp(15));
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10); b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // la pantalla de la alarma va de borde a borde, con las barras del sistema transparentes sobre el degradado
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.parseColor("#1E140E"));
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null)
            getWindow().getInsetsController().setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        else getWindow().getDecorView().setSystemUiVisibility(0);
        pintar();
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> parar(false));

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

    @Override
    protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); pintar(); }

    @Override
    public void onBackPressed() { parar(false); }

    void pintar() {
        String titulo = getIntent().getStringExtra("titulo");
        if (titulo == null || titulo.isEmpty()) titulo = "Es la hora de tu acción";
        int crema = Color.parseColor("#FFF4E6"), oro = Color.parseColor("#F6C978");

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(52), dp(24), dp(36));

        // Arriba: el icono y «Amable Flow», y debajo «TU ACCIÓN COMPROMETIDA»
        LinearLayout marca = new LinearLayout(this);
        marca.setOrientation(LinearLayout.HORIZONTAL); marca.setGravity(Gravity.CENTER);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams llogo = new LinearLayout.LayoutParams(dp(34), dp(34)); llogo.rightMargin = dp(8);
        marca.addView(logo, llogo);
        marca.addView(texto("Amable Flow", 22, Color.WHITE, true));
        col.addView(marca);
        TextView rec = texto("TU ACCIÓN COMPROMETIDA", 14, oro, true);
        rec.setLetterSpacing(0.14f); rec.setPadding(0, dp(4), 0, dp(18)); col.addView(rec);

        // La chica meditando con su pluma, en un círculo con halo; respira despacio y una pluma flota a su lado
        FrameLayout escena = new FrameLayout(this);
        View halo = new View(this);
        GradientDrawable gh = new GradientDrawable(); gh.setShape(GradientDrawable.OVAL);
        gh.setGradientType(GradientDrawable.RADIAL_GRADIENT); gh.setGradientRadius(dp(125));
        gh.setColors(new int[]{Color.argb(120, 246, 201, 120), Color.argb(0, 246, 201, 120)});
        halo.setBackground(gh);
        escena.addView(halo, new FrameLayout.LayoutParams(dp(250), dp(250), Gravity.CENTER));
        ImageView chica = new ImageView(this);
        chica.setImageResource(R.drawable.flow_meditando);
        chica.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable redondo = new GradientDrawable(); redondo.setShape(GradientDrawable.OVAL); redondo.setColor(Color.WHITE);
        chica.setBackground(redondo); chica.setClipToOutline(true);
        escena.addView(chica, new FrameLayout.LayoutParams(dp(190), dp(190), Gravity.CENTER));
        ImageView pluma = new ImageView(this);
        pluma.setImageResource(R.drawable.flow_pluma);
        FrameLayout.LayoutParams lpl = new FrameLayout.LayoutParams(dp(54), dp(46), Gravity.TOP | Gravity.END);
        lpl.topMargin = dp(8); lpl.rightMargin = dp(4);
        escena.addView(pluma, lpl);
        LinearLayout.LayoutParams les = new LinearLayout.LayoutParams(dp(260), dp(250));
        col.addView(escena, les);

        ObjectAnimator respira = ObjectAnimator.ofPropertyValuesHolder(chica,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.045f), PropertyValuesHolder.ofFloat("scaleY", 1f, 1.045f));
        respira.setDuration(3200); respira.setRepeatCount(ValueAnimator.INFINITE); respira.setRepeatMode(ValueAnimator.REVERSE);
        respira.setInterpolator(new AccelerateDecelerateInterpolator()); respira.start();
        ObjectAnimator brillo = ObjectAnimator.ofPropertyValuesHolder(halo,
                PropertyValuesHolder.ofFloat("scaleX", .88f, 1.06f), PropertyValuesHolder.ofFloat("scaleY", .88f, 1.06f), PropertyValuesHolder.ofFloat("alpha", .55f, 1f));
        brillo.setDuration(3200); brillo.setRepeatCount(ValueAnimator.INFINITE); brillo.setRepeatMode(ValueAnimator.REVERSE); brillo.start();
        ObjectAnimator flota = ObjectAnimator.ofPropertyValuesHolder(pluma,
                PropertyValuesHolder.ofFloat("translationY", 0f, dp(26)), PropertyValuesHolder.ofFloat("translationX", 0f, -dp(10)),
                PropertyValuesHolder.ofFloat("rotation", -18f, 14f));
        flota.setDuration(2600); flota.setRepeatCount(ValueAnimator.INFINITE); flota.setRepeatMode(ValueAnimator.REVERSE);
        flota.setInterpolator(new AccelerateDecelerateInterpolator()); flota.start();

        // La hora y la acción
        Calendar ca = Calendar.getInstance();
        int hh = ca.get(Calendar.HOUR_OF_DAY), mi = ca.get(Calendar.MINUTE);
        TextView son = texto((hh % 12 == 1 ? "Es la " : "Son las ") + hh + ":" + (mi < 10 ? "0" : "") + mi, 18, crema, false);
        son.setPadding(0, dp(10), 0, dp(4)); col.addView(son);
        TextView t = texto(titulo, 27, Color.WHITE, true);
        t.setPadding(0, dp(4), 0, dp(26)); t.setLineSpacing(0, 1.08f); col.addView(t);

        // Enterado (para la alarma) · Posponer (se despliega) · Abrir Amable Flow
        Button ent = boton("Enterado", Color.WHITE, Color.parseColor("#3E2C20"), 0);
        ent.setOnClickListener(v -> parar(false));
        col.addView(ent);

        Button pos = boton("Posponer", Color.parseColor("#C07A4E"), Color.WHITE, 0);
        GridLayout rej = new GridLayout(this);
        rej.setColumnCount(3); rej.setVisibility(View.GONE);
        for (int m : MINUTOS) {
            Button o = new Button(this);
            o.setText(m < 60 ? m + " min" : (m / 60) + " h"); o.setAllCaps(false); o.setTextSize(17); o.setTextColor(Color.WHITE);
            o.setStateListAnimator(null);
            GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor("#5A3826")); g.setCornerRadius(dp(14));
            g.setStroke(dp(1.5f), Color.parseColor("#C07A4E")); o.setBackground(g);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0; lp.setMargins(dp(4), dp(8), dp(4), 0); o.setLayoutParams(lp);
            o.setOnClickListener(v -> posponer(m));
            rej.addView(o);
        }
        pos.setOnClickListener(v -> rej.setVisibility(rej.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        col.addView(pos);
        col.addView(rej, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button abrir = boton("Abrir Amable Flow", Color.argb(28, 255, 255, 255), Color.WHITE, Color.argb(110, 255, 255, 255));
        abrir.setOnClickListener(v -> parar(true));
        col.addView(abrir);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        GradientDrawable fondo = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.parseColor("#C98A5E"), Color.parseColor("#9A5F3E"), Color.parseColor("#4A2E20"), Color.parseColor("#1E140E")});
        sv.setBackground(fondo);
        LinearLayout centro = new LinearLayout(this);
        centro.setGravity(Gravity.CENTER); centro.setOrientation(LinearLayout.VERTICAL);
        centro.addView(col, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        sv.addView(centro, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        setContentView(sv);
    }

    /** Vuelve a sonar dentro de m minutos (una sola vez; la alarma de siempre sigue igual). */
    void posponer(int m) {
        String id = getIntent().getStringExtra("id");
        if (id == null) id = "sin-id";
        Intent i = new Intent(this, AlarmaReceiver.class).setAction("SONAR")
                .putExtra("id", id).putExtra("titulo", getIntent().getStringExtra("titulo")).putExtra("pospuesta", true);
        PendingIntent pi = PendingIntent.getBroadcast(this, Alarmas.codigo(id) + 800001, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long cuando = System.currentTimeMillis() + m * 60000L;
        AlarmManager am = getSystemService(AlarmManager.class);
        PendingIntent ver = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        try { am.setAlarmClock(new AlarmManager.AlarmClockInfo(cuando, ver), pi); }
        catch (SecurityException e) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi); }
        Toast.makeText(this, "Te lo vuelvo a recordar en " + (m < 60 ? m + " minutos" : m == 60 ? "1 hora" : (m / 60) + " horas"), Toast.LENGTH_SHORT).show();
        parar(false);
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
