package club.practicaintegral.flow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

/** Suena la alarma (aviso a pantalla completa) y deja programada la siguiente vez. */
public class AlarmaReceiver extends BroadcastReceiver {
    static final String CANAL = "alarmas_flow";
    static final int AVISO = 4242;

    @Override
    public void onReceive(Context c, Intent i) {
        String accion = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(accion) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(accion)) {
            Alarmas.programarTodas(c);
            return;
        }
        if (!"SONAR".equals(accion)) return;
        String id = i.getStringExtra("id"), titulo = i.getStringExtra("titulo");
        if (titulo == null) titulo = "Tu acción comprometida";

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        Uri tono = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        NotificationChannel canal = new NotificationChannel(CANAL, "Alarmas de tus acciones", NotificationManager.IMPORTANCE_HIGH);
        canal.setSound(tono, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        canal.enableVibration(true);
        nm.createNotificationChannel(canal);

        Intent abrir = new Intent(c, AlarmaActivity.class).putExtra("titulo", titulo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pantalla = PendingIntent.getActivity(c, 7, abrir, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CANAL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Amable Flow · tu acción comprometida")
                .setContentText(titulo)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pantalla, true)
                .setContentIntent(pantalla)
                .setAutoCancel(true)
                .build();
        nm.notify(AVISO, n);
        try { c.startActivity(abrir); } catch (Exception ignored) {}

        try { Alarmas.programar(c, id, Alarmas.todas(c).optJSONObject(id)); } catch (Exception ignored) {}
    }
}
