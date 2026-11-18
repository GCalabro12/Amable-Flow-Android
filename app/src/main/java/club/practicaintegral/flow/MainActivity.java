package club.practicaintegral.flow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Amable Flow para Android: muestra la web dentro de la app y añade lo que una web no puede hacer:
 * alarmas de verdad para las acciones comprometidas (suenan aunque la app esté cerrada) y la voz.
 */
public class MainActivity extends Activity {
    static final String WEB = "https://gcalabro12.github.io/Flow/";
    WebView web;
    TextToSpeech tts;

    // La voz de la web (speechSynthesis) no existe dentro de un WebView: se cambia por la voz de Android
    static final String VOZ = "(function(){if(!window.AmableFlowAndroid||window.__vozNativa)return;window.__vozNativa=true;"
            + "var pend={},n=0;window.__ttsFin=function(id){var u=pend[id];delete pend[id];if(u&&u.onend){try{u.onend({})}catch(e){}}};"
            + "window.SpeechSynthesisUtterance=function(t){this.text=t||'';this.lang='es-ES';this.rate=1;this.pitch=1;this.volume=1;this.voice=null;this.onend=null;this.onerror=null;this.onstart=null;this.onboundary=null;};"
            + "window.speechSynthesis={speaking:false,pending:false,paused:false,onvoiceschanged:null,getVoices:function(){return[]},"
            + "speak:function(u){var id=String(++n);pend[id]=u;AmableFlowAndroid.hablar(String(u.text||''),Number(u.rate)||1,id);},"
            + "cancel:function(){for(var k in pend)delete pend[k];AmableFlowAndroid.callar();},pause:function(){},resume:function(){},"
            + "addEventListener:function(){},removeEventListener:function(){}};})();";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUserAgentString(s.getUserAgentString() + " AmableFlowAndroid");
        web.addJavascriptInterface(new Puente(), "AmableFlowAndroid");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (u.toString().startsWith(WEB)) return false;
                abrirFuera(u);
                return true;
            }
            @Override
            public void onPageFinished(WebView v, String url) { v.evaluateJavascript(VOZ, null); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            // window.open (por ejemplo, el calendario de Google): se abre fuera, en el navegador
            @Override
            public boolean onCreateWindow(WebView v, boolean dialogo, boolean usuario, Message msg) {
                WebView tmp = new WebView(MainActivity.this);
                tmp.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
                        abrirFuera(r.getUrl());
                        w.destroy();
                        return true;
                    }
                });
                ((WebView.WebViewTransport) msg.obj).setWebView(tmp);
                msg.sendToTarget();
                return true;
            }
        });

        tts = new TextToSpeech(this, st -> {
            if (st == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("es", "ES"));
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { fin(id); }
            @Override public void onError(String id) { fin(id); }
        });

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        Alarmas.programarTodas(this);
        web.loadUrl(WEB + "?app=android");
    }

    void abrirFuera(Uri u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
    }

    void fin(String id) {
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.__ttsFin&&window.__ttsFin(" + JSONObject.quote(id) + ")", null); });
    }

    void pedirAlarmasExactas() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = getSystemService(AlarmManager.class);
            if (am != null && !am.canScheduleExactAlarms()) {
                try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    /** Lo que la web puede pedir a Android: window.AmableFlowAndroid */
    class Puente {
        /** dias: "L,M,X…" (se repite cada semana) · fechas: "2026-11-20,2026-11-27" (días concretos) · hora: "08:30" */
        @JavascriptInterface
        public void guardarAlarma(String id, String dias, String hora, String titulo, String fechas) {
            Alarmas.guardar(MainActivity.this, id, dias, hora, titulo, fechas);
            runOnUiThread(MainActivity.this::pedirAlarmasExactas);
        }
        @JavascriptInterface
        public void quitarAlarma(String id) { Alarmas.quitar(MainActivity.this, id); }
        @JavascriptInterface
        public void hablar(String texto, float vel, String id) {
            runOnUiThread(() -> {
                if (tts == null) { fin(id); return; }
                tts.setSpeechRate(vel > 0 ? vel : 1f);
                int r = tts.speak(texto, TextToSpeech.QUEUE_ADD, new Bundle(), id);
                if (r != TextToSpeech.SUCCESS) fin(id);
            });
        }
        @JavascriptInterface
        public void callar() { runOnUiThread(() -> { if (tts != null) tts.stop(); }); }
    }
}
