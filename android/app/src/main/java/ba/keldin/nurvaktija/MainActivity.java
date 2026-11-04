package ba.keldin.nurvaktija;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    // All public UI changes are served at this address: new UI needs no new APK.
    private static final String SCREEN_URL = "https://keldin-vaktija.vercel.app/";
    private static final String RELEASE_API = "https://api.github.com/repos/keldin94/keldin-vaktija-/releases/latest";
    private static final String STORE = "nur_tv";
    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersive();
        web = new WebView(this);
        web.setBackgroundColor(0xff052c30);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        WebSettings options = web.getSettings();
        options.setJavaScriptEnabled(true);
        options.setDomStorageEnabled(true);
        options.setCacheMode(WebSettings.LOAD_DEFAULT);
        options.setMediaPlaybackRequiresUserGesture(false);
        options.setAllowFileAccess(false);
        options.setAllowContentAccess(false);
        web.addJavascriptInterface(new DeviceBridge(), "NurAndroid");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equals(uri.getScheme()) && "keldin-vaktija.vercel.app".equals(uri.getHost()));
            }
            @Override public void onReceivedError(WebView v, android.webkit.WebResourceRequest req,
                  android.webkit.WebResourceError err) {
                // WebView may show Android's network error when the site is not cached.
                // Check the TV's Wi-Fi; future versions include a bundled offline shell.
            }
        });
        setContentView(web);
        web.loadUrl(SCREEN_URL);
        downloadId = getPreferences().getLong("downloadId", -1L);
        downloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()) &&
                    intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    offerInstallation();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
        // Screen is reloaded infrequently to pick up web UI changes.
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (web != null) web.reload();
                handler.postDelayed(this, 6L * 60 * 60 * 1000);
            }
        }, 6L * 60 * 60 * 1000);
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(STORE, MODE_PRIVATE);
    }

    @Override protected void onResume() {
        super.onResume();
        enterImmersive();
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private final class DeviceBridge {
        @JavascriptInterface public void checkForUpdates() { runOnUiThread(() -> checkForUpdates()); }
    }

    private void openSettings() {
        if (web != null) web.evaluateJavascript(
            "if(window.openNurSettings){window.openNurSettings()}", null
        );
    }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS) {
            openSettings(); return true;
        }
        return super.onKeyDown(code, event);
    }

    @Override public void onBackPressed() {
        // First close the Settings panel if open; otherwise remain in TV mode.
        if (web != null) web.evaluateJavascript(
            "(function(){var m=document.getElementById('modal');" +
            "if(m&&m.classList.contains('open'))m.classList.remove('open');})()", null
        );
    }

    private void notice(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("U redu", (d,w)->{}).show();
    }

    private static int[] versions(String x) {
        String v = x.replaceFirst("^[vV]", "").split("[-+]",2)[0];
        String[] fields = v.split("\\.");
        int[] nums = new int[]{0,0,0};
        for (int i = 0; i < Math.min(3,fields.length); i++) {
            try { nums[i] = Integer.parseInt(fields[i]); } catch(Exception ignored) {}
        }
        return nums;
    }

    private static boolean newer(String remote, String installed) {
        int[] a=versions(remote), b=versions(installed);
        for(int i=0;i<3;i++){ if(a[i]>b[i])return true; if(a[i]<b[i])return false; }
        return false;
    }

    private void checkForUpdates() {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(RELEASE_API).openConnection();
                conn.setConnectTimeout(9000); conn.setReadTimeout(9000);
                conn.setRequestProperty("Accept","application/vnd.github+json");
                conn.setRequestProperty("User-Agent","NurVaktija-AndroidTV");
                if (conn.getResponseCode() != 200) throw new Exception("Nema objavljene stabilne verzije.");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream s=conn.getInputStream()) {
                    byte[] chunk=new byte[4096]; int n;
                    while((n=s.read(chunk))!=-1)bytes.write(chunk,0,n);
                } finally { conn.disconnect(); }
                JSONObject release = new JSONObject(bytes.toString("UTF-8"));
                String tag = release.optString("tag_name", "");
                String current = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
                JSONArray assets=release.optJSONArray("assets");
                String apk=null;
                if(assets!=null)for(int i=0;i<assets.length();i++){
                    JSONObject a=assets.getJSONObject(i);
                    if("Nur-Vaktija.apk".equals(a.optString("name")))apk=a.optString("browser_download_url");
                }
                final String downloadUrl=apk, version=tag;
                runOnUiThread(()->{
                    if(!newer(version,current)) {
                        notice("Nur Vaktija", "Aplikacija je ažurna (verzija "+current+").");
                    } else if (downloadUrl==null) {
                        notice("Ažuriranje", "Pronađena je nova verzija, ali instalacijski APK nije objavljen.");
                    } else {
                        new AlertDialog.Builder(this).setTitle("Dostupna je verzija "+version)
                            .setMessage("Preuzeti i instalirati novu verziju? Android će zatražiti potvrdu instalacije.")
                            .setNegativeButton("Kasnije",(d,w)->{})
                            .setPositiveButton("Preuzmi",(d,w)->startDownload(downloadUrl))
                            .show();
                    }
                });
            } catch(Exception e) {
                runOnUiThread(()->notice("Provjera ažuriranja", "Provjera nije uspjela: "+e.getMessage()));
            }
        }).start();
    }

    private void startDownload(String url) {
        try {
            DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Nur Vaktija — ažuriranje");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "Nur-Vaktija-update.apk");
            downloadId=dm.enqueue(request);
            getPreferences().edit().putLong("downloadId",downloadId).apply();
            notice("Preuzimanje", "Nova verzija se preuzima. Nakon preuzimanja zatražit će se potvrda instalacije.");
        } catch (Exception e) { notice("Greška", e.getMessage()); }
    }

    private void offerInstallation() {
        if(downloadId<0)return;
        DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query q=new DownloadManager.Query().setFilterById(downloadId);
        try(android.database.Cursor c=dm.query(q)){
            if(c==null||!c.moveToFirst())return;
            int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if(status==DownloadManager.STATUS_FAILED){
                getPreferences().edit().remove("downloadId").apply();
                downloadId=-1;
                notice("Greška", "Preuzimanje APK-a nije uspjelo.");
                return;
            }
            if(status!=DownloadManager.STATUS_SUCCESSFUL)return;
        }
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setTitle("Dozvola za ažuriranje")
                .setMessage("Potrebna je jednokratna dozvola za instaliranje iz ove aplikacije.")
                .setPositiveButton("Postavke",(d,w)->{
                    Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:"+getPackageName()));startActivity(i);
                }).setNegativeButton("Kasnije",(d,w)->{}).show();
            return;
        }
        Uri uri=dm.getUriForDownloadedFile(downloadId);
        if(uri==null){notice("Greška","Preuzeta datoteka nije dostupna.");return;}
        Intent install=new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri,"application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        try{startActivity(install);}catch(Exception e){notice("Greška","Instalacija nije dostupna: "+e.getMessage());}
    }

    @Override protected void onDestroy() {
        if(downloadReceiver!=null)unregisterReceiver(downloadReceiver);
        handler.removeCallbacksAndMessages(null);
        if(web!=null){web.destroy();web=null;}
        super.onDestroy();
    }
}
