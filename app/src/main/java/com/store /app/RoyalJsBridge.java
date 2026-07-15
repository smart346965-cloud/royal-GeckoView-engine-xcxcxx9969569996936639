package com.store.app;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.PromptDelegate.TextPrompt;
import org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse;

/**
=========================================================
👑 ROYAL JS BRIDGE (GeckoView Native Interface Channel)
=========================================================
Channeling high-performance telemetry, page warmups, and splash
control from Gecko runtime to Android native runtime.
*/
public class RoyalJsBridge implements GeckoSession.PromptDelegate {

    private static final String TAG = "RoyalJsBridge";
    private final GeckoSession geckoSession;
    private Runnable onHideSplashCallback;

    public RoyalJsBridge(GeckoSession geckoSession) {
        this.geckoSession = geckoSession;
        
        // 👁️ تسجيل الهيكل الشجري للمحركات داخل نظام التشخيص الملكي عند بناء الجسر  
        RoyalPanopticon.registerDependency("WebChromeEngine", "JS-BridgeChannel");  
        RoyalPanopticon.registerDependency("JS-BridgeChannel", "TapWarmupEngine");
    }

    public void setOnHideSplashCallback(Runnable callback) {
        this.onHideSplashCallback = callback;
    }

    /**
    🌉 تثبيت الجسر وحقن كائن الاتصال البرمجي (window.RoyalBridge) داخل المتصفح
    */
    public void inject() {
        if (geckoSession == null) return;

        String jsPayload = "(function() {" +
                "if (window.RoyalBridge) return;" +
                "window.RoyalBridge = {" +
                "  warmup: function(url) { prompt('RoyalBridge:warmup', url); }," +
                "  scrollHint: function(v) { prompt('RoyalBridge:scrollHint', String(v)); }," +
                "  log: function(msg) { prompt('RoyalBridge:log', msg); }," +
                "  hideSplash: function() { prompt('RoyalBridge:hideSplash', ''); }," +
                "  reportBrowserState: function(d, f, m, l) { prompt('RoyalBridge:reportBrowserState', d + ',' + f + ',' + m + ',' + l); }," +
                "  inspect: function() { prompt('RoyalBridge:inspect', ''); }" +
                "};" +
                "})();";

        geckoSession.loadUri("javascript:void(" + jsPayload + ")");
    }

    /**
    📥 اعتراض رسائل الـ prompt وتوجيهها للميثودز النيتف المقابلة
    */
    @Nullable
    @Override
    public GeckoResult<PromptResponse> onTextPrompt(@NonNull GeckoSession session, @NonNull TextPrompt prompt) {
        String message = prompt.message;

        if (message != null && message.startsWith("RoyalBridge:")) {
            String action = message.substring("RoyalBridge:".length());
            String payload = prompt.defaultValue;

            handleBridgeAction(action, payload);  
            
            // تأكيد إلغاء النافذة الصامت لكي لا تظهر للمستخدم نهائياً  
            return GeckoResult.fromValue(prompt.confirm(""));
        }

        return null; // اترك النوافذ العادية ليتعامل معها النظام تلقائياً
    }

    private void handleBridgeAction(String action, String payload) {
        switch (action) {
            case "warmup":
                warmup(payload);
                break;
            case "scrollHint":
                try {
                    scrollHint(Integer.parseInt(payload));
                } catch (Exception ignored) {}
                break;
            case "log":
                log(payload);
                break;
            case "hideSplash":
                hideSplash();
                break;
            case "reportBrowserState":
                try {
                    String[] parts = payload.split(",");
                    int domNodes = Integer.parseInt(parts[0]);
                    int fps = Integer.parseInt(parts[1]);
                    long jsMemoryMB = Long.parseLong(parts[2]);
                    int longTasks = Integer.parseInt(parts[3]);
                    reportBrowserState(domNodes, fps, jsMemoryMB, longTasks);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to sync browser state", e);
                }
                break;
            case "inspect":
                inspect();
                break;
        }
    }

    /**
    🚀 Network Warmup
    */
    public void warmup(String url) {
        try {
            RoyalPanopticon.pulse("TapWarmupEngine");
            
            long start = System.currentTimeMillis();  
            RoyalNetworkEngine.warmupLink(url);  
            long duration = System.currentTimeMillis() - start;  
            
            RoyalPanopticon.recordExecution("TapWarmupEngine", duration, true, 0);
        } catch (Exception e) {
            Log.e(TAG, "Warmup failed", e);
            RoyalPanopticon.recordExecution("TapWarmupEngine", 0, false, 0);
        }
    }

    /**
    🌊 Scroll velocity hint
    */
    public void scrollHint(int velocity) {
        try {
            RoyalPanopticon.pulse("JS-BridgeChannel");
            Log.d(TAG, "Scroll velocity: " + velocity);
        } catch (Exception e) {
            Log.e(TAG, "scrollHint error", e);
        }
    }

    /**
    🧠 JS diagnostic channel
    */
    public void log(String message) {
        Log.d(TAG, "JS: " + message);
        RoyalPanopticon.pulse("WebChromeEngine");
    }

    /**
    🎭 Visual Completeness Signal
    */
    public void hideSplash() {
        if (onHideSplashCallback != null && geckoSession != null) {
            // تنفيذ كولباك إخفاء شاشة السبلاش بأمان تام على الـ UI Thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(onHideSplashCallback);
        }
    }

    /**
    👁️ Panopticon Telemetry Receiver
    */
    public void reportBrowserState(int domNodes, int fps, long jsMemoryMB, int longTasks) {
        try {
            RoyalPanopticon.syncBrowserState(domNodes, fps, jsMemoryMB, longTasks);
            RoyalPanopticon.pulse("WebChromeEngine");
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync browser state", e);
        }
    }

    public void inspect() {
        try {
            String report = RoyalPanopticon.buildReport();
            
            report = report  
                    .replace("\\", "\\\\")  
                    .replace("`", "\\`")  
                    .replace("$", "\\$");  
            
            final String js = "console.log(`" + report + "`);";  
            if (geckoSession != null) {  
                geckoSession.loadUri("javascript:void(" + js + ")");  
            }  
        } catch (Exception e) {  
            Log.e(TAG, "Inspect failed", e);  
        }
    }

    /**
    🔁 Native → JS callback
    */
    public void dispatchToJS(String script) {
        if (geckoSession == null) return;
        geckoSession.loadUri("javascript:void(" + script + ")");
    }

    /**
    🔌 تثبيت الجسر البرمجي كـ PromptDelegate للجلسة
    */
    public void install() {
        if (geckoSession != null) {
            geckoSession.setPromptDelegate(this);
        }
    }
    }
