package club.practicaintegral.flow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Iterator;

/**
 * Alarmas de las acciones comprometidas. Las programa el reloj de Android (setAlarmClock),
 * así suenan aunque Amable Flow esté cerrada o el móvil bloqueado.
 */
public class Alarmas {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("alarmas", Context.MODE_PRIVATE); }

    static JSONObject todas(Context c) {
        try { return new JSONObject(prefs(c).getString("lista", "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    static void guardar(Context c, String id, String dias, String hora, String titulo, String fechas) {
        try {
            JSONObject t = todas(c), a = new JSONObject();
            a.put("dias", dias == null ? "" : dias);
            a.put("hora", hora == null ? "09:00" : hora);
            a.put("titulo", titulo == null ? "Tu acción comprometida" : titulo);
            a.put("fechas", fechas == null ? "" : fechas);
            t.put(id, a);
            prefs(c).edit().putString("lista", t.toString()).apply();
            programar(c, id, a);
        } catch (Exception ignored) {}
    }

    static void quitar(Context c, String id) {
        cancelar(c, id);
        try {
            JSONObject t = todas(c);
            t.remove(id);
            prefs(c).edit().putString("lista", t.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Vuelve a programar todas (al abrir la app, al reiniciar el móvil y al actualizar la app). */
    static void programarTodas(Context c) {
        JSONObject t = todas(c);
        Iterator<String> k = t.keys();
        while (k.hasNext()) { String id = k.next(); programar(c, id, t.optJSONObject(id)); }
    }

    static int codigo(String id) { return Math.abs(id.hashCode() % 800000) + 1; }

    /** La próxima vez que debe sonar (ms), o -1 si ya no queda ninguna. */
    static long proxima(JSONObject a) {
        String[] hm = a.optString("hora", "09:00").split(":");
        int h = Integer.parseInt(hm[0].trim()), m = Integer.parseInt(hm[1].trim());
        long ahora = System.currentTimeMillis();
        String fechas = a.optString("fechas", "").trim();
        if (!fechas.isEmpty()) {
            long mejor = -1;
            for (String f : fechas.split(",")) {
                String[] p = f.trim().split("-");
                if (p.length < 3) continue;
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), h, m, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long t = cal.getTimeInMillis();
                if (t > ahora + 1000 && (mejor < 0 || t < mejor)) mejor = t;
            }
            return mejor;
        }
        String dias = a.optString("dias", ""), letras = "LMXJVSD";
        for (int i = 0; i < 8; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, i);
            cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= ahora + 1000) continue;
            char letra = letras.charAt((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7); // lunes = L … domingo = D
            if (dias.isEmpty() || dias.indexOf(letra) >= 0) return cal.getTimeInMillis();
        }
        return -1;
    }

    static void programar(Context c, String id, JSONObject a) {
        if (a == null) return;
        cancelar(c, id);
        long cuando = proxima(a);
        if (cuando < 0) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Intent i = new Intent(c, AlarmaReceiver.class).setAction("SONAR")
                .putExtra("id", id).putExtra("titulo", a.optString("titulo", "Tu acción comprometida"));
        PendingIntent pi = PendingIntent.getBroadcast(c, codigo(id), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent ver = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(cuando, ver), pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi);
        }
    }

    static void cancelar(Context c, String id) {
        Intent i = new Intent(c, AlarmaReceiver.class).setAction("SONAR");
        PendingIntent pi = PendingIntent.getBroadcast(c, codigo(id), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pi != null) { c.getSystemService(AlarmManager.class).cancel(pi); pi.cancel(); }
    }
}
