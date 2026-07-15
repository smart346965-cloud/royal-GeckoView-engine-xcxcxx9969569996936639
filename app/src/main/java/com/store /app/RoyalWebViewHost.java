package com.store.app;

import android.app.Activity;
import android.content.Context;
import android.content.MutableContextWrapper;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewRenderProcess;
import androidx.webkit.WebViewRenderProcessClient;

/**
 * =========================================================
 * 👑 ROYAL WEBVIEW HOST (The Immortal Engine Core V3)
 * =========================================================
 * Architecture: Thread-Safe Singleton, V8 Pre-Warmed,
 * Memory-Leak Proof (Soft Restart), Crash Resilient.
 */
public final class RoyalWebViewHost {

    private static final String TAG = "RoyalWebViewHost";

    // ==========================================
    // 1️⃣ Core Instance Layer
    // ==========================================
    private static WebView webViewInstance;
    private static MutableContextWrapper contextWrapper;
    private static boolean isInitialized = false;

    // ==========================================
    // 2️⃣ Engine State & Telemetry
    // ==========================================
    private static long creationTime = 0;
    private static int attachCount = 0;
    private static int detachCount = 0;

    // 🕒 Soft Restart System (طبقة حماية الذاكرة)
    private static long lastRestartTime = 0;
    private static final long MAX_UPTIME = 3 * 60 * 60 * 1000L; // 3 ساعات

    // 🌉 مسار الوصول المباشر للجسر الملكي
    private static RoyalJsBridge jsBridgeInstance;

    private RoyalWebViewHost() {}

    /**
     * 🚀 CREATE: يولد الويب فيو ويسخنه في الخلفية (Thread-Safe)
     */
    public static synchronized void create(Context applicationContext) {
        // 🛡️ التحسين 1: فرض التنفيذ على UI Thread لمنع الانهيار الصامت
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("❌ FATAL: WebView must be created on UI thread");
        }

        if (isInitialized && webViewInstance != null) {
            return;
        }

        Log.i(TAG, "⚙️ Initiating Royal WebView Host...");
        creationTime = System.currentTimeMillis();
        lastRestartTime = System.currentTimeMillis();

        contextWrapper = new MutableContextWrapper(applicationContext.getApplicationContext());

        try {
            webViewInstance = new WebView(contextWrapper);
            isInitialized = true;

            if (!BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(false);
            }

            // 🛡️ التحسين 2: تفعيل Hardware Acceleration للرسوميات
            webViewInstance.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            // تطبيق الإعدادات الاحترافية
            RoyalHybridEngine.prime(webViewInstance, applicationContext);

            // 🚀 تجهيز ملفات الحقن مرة واحدة داخل الذاكرة
            WebEnhancer.preload(applicationContext);

            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)) {

                WebViewCompat.setWebViewRenderProcessClient(
                        webViewInstance,
                        Runnable::run,
                        new WebViewRenderProcessClient() {

                            @Override
                            public void onRenderProcessResponsive(
                                    WebView view,
                                    WebViewRenderProcess renderer) {
                            }

                            @Override
                            public void onRenderProcessUnresponsive(
                                    WebView view,
                                    WebViewRenderProcess renderer) {

                                RoyalNetworkEngine.notifyRenderStart();
                            }
                        });
            }

            // 🌐 تثبيت محرك الشبكة الملكي
            RoyalNetworkEngine.install(applicationContext);

            // 🌉 حقن الجسر الملكي وحفظ النسخة لربطها بالواجهة لاحقاً
            jsBridgeInstance = new RoyalJsBridge(webViewInstance);
            webViewInstance.addJavascriptInterface(jsBridgeInstance, "RoyalJsBridge");

            // 🔥 التحسين 3: تسخين محرك V8 مباشرة بدلاً من about:blank
            webViewInstance.evaluateJavascript("(function(){return 'warm';})()", null);

            Log.i(TAG, "✅ Immortal WebView Created, Bridge Injected & V8 Warmed Up.");
        } catch (Exception e) {
            Log.e(TAG, "❌ FATAL: Failed to create WebView instance.", e);
        }
    }

    /**
     * 🔗 ATTACH: يربط الويب فيو بالشاشة الحالية (Thread-Safe)
     */
    public static synchronized WebView attach(Activity activity) {
        // 🛡️ طبقة 3: Soft Restart System (الفحص الذكي قبل الربط)
        checkSoftRestart(activity.getApplicationContext());

        if (webViewInstance == null) {
            throw new IllegalStateException("❌ Call create() before attach()!");
        }

        Log.i(TAG, "🔗 Attaching WebView to: " + activity.getClass().getSimpleName());

        // 🛡️ طبقة 1: Plugin Rebinding System (يحدث تلقائياً بتغيير السياق هنا)
        contextWrapper.setBaseContext(activity);

        safeRemoveFromParent();

        webViewInstance.onResume();
        webViewInstance.resumeTimers();

        attachCount++;
        return webViewInstance;
    }

    /**
     * 🧲 DETACH: يفصل الويب فيو ويخبئه في الذاكرة (Thread-Safe)
     */
    public static synchronized void detach() {
        if (webViewInstance == null) return;

        Log.i(TAG, "🧲 Detaching WebView (Entering Hibernation)...");

        safeRemoveFromParent();

        if (contextWrapper != null) {
            contextWrapper.setBaseContext(webViewInstance.getContext().getApplicationContext());
        }

        webViewInstance.onPause();
        webViewInstance.pauseTimers();

        detachCount++;
    }

    /**
     * 💣 DESTROY: تدمير آمن تماماً
     */
    public static synchronized void destroy() {
        if (webViewInstance != null) {
            Log.w(TAG, "💣 Destroying Royal WebView Host.");
            safeRemoveFromParent();

            try {
                webViewInstance.stopLoading();
                webViewInstance.loadUrl("about:blank");
                webViewInstance.pauseTimers();
                webViewInstance.destroy();
            } catch (Exception ignored) {}

            webViewInstance = null;
            contextWrapper = null;
            isInitialized = false;
        }

        // 🔥 الإنقاذ الجراحي: تصفير حالة الإعدادات لكي تُطبق على الويب فيو الجديد!
        RoyalHybridEngine.reset();
    }

    // 👑 دالة جلب الويب فيو الخالد
    public static WebView get() {
        return webViewInstance;
    }

    // ==========================================
    // 🛡️ أنظمة الحماية الداخلية (Internal Guards)
    // ==========================================

    /**
     * 🔄 Soft Restart System (إعادة التشغيل لحماية الرام)
     */
    public static synchronized void checkSoftRestart(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastRestartTime > MAX_UPTIME) {
            Log.w(TAG, "🔄 Soft restarting WebView to clear Chromium Memory Leak (3 Hours Reached).");
            destroy();
            create(context);
        }
    }

    private static void safeRemoveFromParent() {
        if (webViewInstance != null) {
            ViewParent parent = webViewInstance.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webViewInstance);
            }
        }
    }

    public static boolean isReady() {
        return isInitialized && webViewInstance != null;
    }

    public static void stats() {
        Log.i(TAG, "📊 === ROYAL ENGINE TELEMETRY === 📊");
        Log.i(TAG, "Status     : " + (isInitialized ? "ONLINE 🟢" : "OFFLINE 🔴"));
        Log.i(TAG, "Uptime     : " + (System.currentTimeMillis() - creationTime) + "ms");
        Log.i(TAG, "Attaches   : " + attachCount);
        Log.i(TAG, "Detaches   : " + detachCount);
        Log.i(TAG, "====================================");
    }

    // 👑 دالة جلب الجسر البرمجي لربطه بالواجهة
    public static RoyalJsBridge getBridge() {
        return jsBridgeInstance;
    }
                }
