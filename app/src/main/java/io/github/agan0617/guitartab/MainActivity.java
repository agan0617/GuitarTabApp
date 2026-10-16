package io.github.agan0617.guitartab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * K吉他 App：整個畫面是一個 WebView，直接開線上的網頁版（GitHub Pages），做法跟魔塔、K書吧 App 一樣。
 * 網頁版改了，App 下次打開就是新的，不用重新打包 APK。
 * 看譜時手在彈吉他，所以 App 開著的時候螢幕一律不關（WebView 不一定支援網頁的 Wake Lock）；
 * 藍牙翻頁踏板送的按鍵直接交給網頁處理。
 */
public class MainActivity extends Activity {

    static final String HOME = "https://agan0617.github.io/GuitarTab/";
    private static final String HOST = "agan0617.github.io";

    private WebView web;
    private boolean pageFailed;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Window w = getWindow();
        w.setStatusBarColor(0xFF1F2933);
        w.setNavigationBarColor(0xFF1F2933);
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF1F2933);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(false);
        s.setTextZoom(100);
        s.setAllowFileAccess(false);
        s.setUserAgentString(s.getUserAgentString() + " GuitarTabApp/" + BuildConfigVersion.NAME);
        // 有網路就一律抓最新網頁（GitHub Pages 會被快取 10 分鐘）；抓回來的仍會寫進快取，離線時才用快取裡的
        s.setCacheMode(isOnline() ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_CACHE_ELSE_NETWORK);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                if (url != null && url.startsWith("http")) pageFailed = false;
            }

            // 抓不到最新網頁：先退回快取；快取也沒有就顯示重試頁
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (!r.isForMainFrame()) return;
                pageFailed = true;
                WebSettings ws = v.getSettings();
                if (ws.getCacheMode() != WebSettings.LOAD_CACHE_ELSE_NETWORK) {
                    ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    v.loadUrl(HOME);
                } else {
                    v.loadDataWithBaseURL(null,
                            "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                            + "<body style='font-family:sans-serif;text-align:center;padding:30vh 24px 0;color:#aaa;background:#1f2933'>"
                            + "<p>連不上網路，K吉他打不開。</p><p><a href='" + HOME + "' style='color:#4fb386'>網路恢復後點這裡重試</a></p></body>",
                            "text/html", "utf-8", null);
                }
            }

            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (HOST.equals(u.getHost())) return false;
                // 產 token 的 GitHub 頁面等外部連結交給瀏覽器
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });

        if (saved != null) web.restoreState(saved); else web.loadUrl(HOME);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override protected void onResume() {
        super.onResume();
        // 上次沒載成功、現在有網路了：抓最新版重來
        if (pageFailed && isOnline() && web != null) {
            pageFailed = false;
            web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            web.loadUrl(HOME);
        }
        hideSystemBars();
    }

    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) hideSystemBars();
    }

    private boolean isOnline() {
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkCapabilities nc = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /** 看譜用全螢幕，譜能多佔一點（從邊緣滑一下可以叫出系統列） */
    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        // 返回鍵先交給網頁（看譜中回譜架、關連線設定）；在譜架上才退到背景
        web.evaluateJavascript("window.GT&&GT.onBack?GT.onBack():false", v -> {
            if (!"true".equals(v)) moveTaskToBack(true);
        });
    }

    @Override protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
