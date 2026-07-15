package com.store.app;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class RoyalJsBridge {

    private static final String TAG = "RoyalJsBridge";
    private final WebView webView;
    private Runnable onHideSplashCallback;

    public RoyalJsBridge(WebView webView) {
        this.webView = webView;
        
        // 👁️ تسجيل الهيكل الشجري للمحركات داخل نظام التشخيص الملكي عند بناء الجسر
        RoyalPanopticon.registerDependency("WebChromeEngine", "JS-BridgeChannel");
        RoyalPanopticon.registerDependency("JS-BridgeChannel", "TapWarmupEngine");
    }

    public void setOnHideSplashCallback(Runnable callback) {
        this.onHideSplashCallback = callback;
    }

    /**
     * 🚀 Network Warmup
     * يتم استدعاؤه من TapEngine قبل النقر
     */
    @JavascriptInterface
    public void warmup(String url) {
        try {
            // 👁️ إرسال نبضة للمحرك تفيد بأنه يشتغل الآن بنشاط وكفاءة
            RoyalPanopticon.pulse("TapWarmupEngine");
            
            long start = System.currentTimeMillis();
            RoyalNetworkEngine.warmupLink(url);
            long duration = System.currentTimeMillis() - start;
            
            // تسجيل سرعة تتبع تسخين الروابط
            RoyalPanopticon.recordExecution("TapWarmupEngine", duration, true, 0);
        } catch (Exception e) {
            Log.e(TAG, "Warmup failed", e);
            RoyalPanopticon.recordExecution("TapWarmupEngine", 0, false, 0);
        }
    }

    /**
     * 🌊 Scroll velocity hint
     */
    @JavascriptInterface
    public void scrollHint(int velocity) {
        try {
            // 👁️ إشعار المحرك بنبضة التمرير الحالية
            RoyalPanopticon.pulse("JS-BridgeChannel");
            Log.d(TAG, "Scroll velocity: " + velocity);
        } catch (Exception e) {
            Log.e(TAG, "scrollHint error", e);
        }
    }

    /**
     * 🧠 JS diagnostic channel
     */
    @JavascriptInterface
    public void log(String message) {
        Log.d(TAG, "JS: " + message);
        RoyalPanopticon.pulse("WebChromeEngine");
    }

    /**
     * 🎭 Visual Completeness Signal
     * يُستدعى من الجافاسكريبت عندما يكتمل رسم الموقع بالكامل
     */
    @JavascriptInterface
    public void hideSplash() {
        if (onHideSplashCallback != null) {
            if (webView != null) {
                webView.post(onHideSplashCallback);
            }
        }
    }

    /**
     * 👁️‍عون Panopticon Telemetry Receiver
     * يستقبل الحالة الصحية للمتصفح من الجافاسكريبت ويرسلها للعقل المدبر
     */
    @JavascriptInterface
    public void reportBrowserState(int domNodes, int fps, long jsMemoryMB, int longTasks) {
        try {
            RoyalPanopticon.syncBrowserState(domNodes, fps, jsMemoryMB, longTasks);
            RoyalPanopticon.pulse("WebChromeEngine");
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync browser state", e);
        }
    }

    @JavascriptInterface
    public void inspect() {
        try {
            String report = RoyalPanopticon.buildReport();

            // نهرب النص عشان ما يكسر الجافاسكريبت
            report = report
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$");

            final String js = "console.log(`" + report + "`);";
            webView.post(() -> webView.evaluateJavascript(js, null));

        } catch (Exception e) {
            Log.e("RoyalJsBridge", "Inspect failed", e);
        }
    }

    /**
     * 🔁 Native → JS callback
     */
    public void dispatchToJS(String script) {
        if (webView == null) return;
        webView.post(() -> webView.evaluateJavascript(script, null));
    }
}
