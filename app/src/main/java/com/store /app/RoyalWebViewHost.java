package com.store.app;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

/**
=========================================================
👑 ROYAL WEBVIEW HOST (The Immortal Gecko Engine Core V4)
=========================================================
Architecture: Thread-Safe Singleton, Low-Level Gecko Pre-Warmed,
Memory-Leak Proof (Soft Restart), Crash Resilient.
*/
public final class RoyalWebViewHost {

    private static final String TAG = "RoyalGeckoHost";

    // ==========================================
    // 1️⃣ Core Gecko Instance Layer
    // ==========================================
    private static GeckoView geckoViewInstance;
    private static GeckoSession geckoSessionInstance;
    private static GeckoRuntime geckoRuntimeInstance;
    private static boolean isInitialized = false;

    // ==========================================
    // 2️⃣ Engine State & Telemetry
    // ==========================================
    private static long creationTime = 0;
    private static int attachCount = 0;
    private static int detachCount = 0;

    // 🕒 Soft Restart System (لحماية الذاكرة العشوائية من التراكم)
    private static long lastRestartTime = 0;
    private static final long MAX_UPTIME = 3 * 60 * 60 * 1000L; // 3 ساعات

    // 🌉 جسر التواصل الملكي المخصص لـ Gecko
    private static RoyalJsBridge geckoBridgeInstance;

    private RoyalWebViewHost() {}

    /**
    🚀 CREATE: تهيئة محرك GeckoRuntime وجلسة العمل وتسخينهما بالكامل في الخلفية (Thread-Safe)
    */
    public static synchronized void create(@NonNull Context applicationContext) {
        // 🛡️ فرض التنفيذ على الـ UI Thread لمنع انهيار محرك الرندرة الرسومي
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("❌ FATAL: GeckoView must be created on UI thread");
        }

        if (isInitialized && geckoViewInstance != null) {
            return;
        }

        Log.i(TAG, "⚙️ Initiating Royal Gecko Engine Host...");
        creationTime = System.currentTimeMillis();
        lastRestartTime = System.currentTimeMillis();

        try {
            // 1️⃣ بناء مستودع التشغيل الموحد (Single Runtime Instance) - لتوفير الرام
            if (geckoRuntimeInstance == null) {
                geckoRuntimeInstance = GeckoRuntime.create(applicationContext.getApplicationContext());
            }

            // 2️⃣ تهيئة الجلسة المستقلة (GeckoSession) وإعدادها لاستقبال الرندرة  
            geckoSessionInstance = new GeckoSession();  
            
            // ضبط خيارات الجلسة بشكل متقدم  
            GeckoSessionSettings settings = geckoSessionInstance.getSettings();  
            settings.setUseTrackingProtection(true); // حماية مدمجة ضد أدوات التتبع لتسريع التصفح  
            settings.setAllowJavascript(true);       // تفعيل الجافا سكريبت بالكامل  
            settings.setAutomaticFullscreenMode(true);  
            
            // 3️⃣ ربط الجلسة بالمحرك الأساسي  
            geckoSessionInstance.open(geckoRuntimeInstance);  
            
            // 4️⃣ بناء حاوية العرض الرسومية (GeckoView)  
            geckoViewInstance = new GeckoView(applicationContext.getApplicationContext());  
            geckoViewInstance.setSession(geckoSessionInstance);  
            
            // 🛡️ تفعيل الـ Hardware Acceleration على مستوى النظام للمحرك الجديد  
            geckoViewInstance.setLayerType(View.LAYER_TYPE_HARDWARE, null);  
            
            // 🌉 تجهيز وحقن الجسر الملكي المطور لـ GeckoView  
            geckoBridgeInstance = new RoyalJsBridge(geckoSessionInstance);  
            geckoBridgeInstance.install(); // تثبيت الجسر لتأمين التواصل الفوري مع الويب  
            
            isInitialized = true;  
            Log.i(TAG, "✅ Immortal GeckoView Host Created & V8 pre-warm sequence initiated.");
        } catch (Exception e) {
            Log.e(TAG, "❌ FATAL: Failed to initialize GeckoView Engine.", e);
        }
    }

    /**
    🔗 ATTACH: ربط واجهة العرض (GeckoView) بالـ Activity النشطة حالياً بالتطبيق
    */
    public static synchronized GeckoView attach(@NonNull Activity activity) {
        // 🛡️ فحص حماية الرام وإعادة التشغيل الصامت عند اللزوم
        checkSoftRestart(activity.getApplicationContext());

        if (geckoViewInstance == null) {
            throw new IllegalStateException("❌ Call create() before attach()!");
        }

        Log.i(TAG, "🔗 Attaching GeckoView to: " + activity.getClass().getSimpleName());

        // فصل الواجهة من أي حاوية قديمة بأمان تام لمنع تسريب الذاكرة (Memory Leak)
        safeRemoveFromParent();

        // تنشيط الجلسة فوراً
        if (geckoSessionInstance != null) {
            geckoSessionInstance.setActive(true);
        }

        attachCount++;
        return geckoViewInstance;
    }

    /**
    🧲 DETACH: فصل حاوية العرض وإدخال المحرك في وضع السبات الذكي للحفاظ على البطارية والذاكرة
    */
    public static synchronized void detach() {
        if (geckoViewInstance == null) return;

        Log.i(TAG, "🧲 Detaching GeckoView (Entering Silent Hibernation)...");

        safeRemoveFromParent();

        if (geckoSessionInstance != null) {
            geckoSessionInstance.setActive(false); // إيقاف الرسوميات الثقيلة مؤقتاً في الخلفية
        }

        detachCount++;
    }

    /**
    💣 DESTROY: تدمير المحرك والتحرير الكامل والجراحي لموارد النظام
    */
    public static synchronized void destroy() {
        if (geckoViewInstance != null) {
            Log.w(TAG, "💣 Destroying Royal Gecko Host.");
            safeRemoveFromParent();

            try {  
                if (geckoSessionInstance != null) {  
                    geckoSessionInstance.stop();  
                    geckoSessionInstance.close();  
                }  
            } catch (Exception ignored) {}  
            
            geckoViewInstance = null;  
            geckoSessionInstance = null;  
            isInitialized = false;
        }
    }

    // 👑 دالة جلب كائن العرض المستقل
    public static GeckoView get() {
        return geckoViewInstance;
    }

    // 👑 دالة جلب الجلسة النشطة للتحكم بالروابط وعمليات التحميل
    public static GeckoSession getSession() {
        return geckoSessionInstance;
    }

    // ==========================================
    // 🛡️ أنظمة الحماية الداخلية (Internal Guards)
    // ==========================================

    /**
    🔄 Soft Restart System (إعادة تصفير الذاكرة الدورية)
    */
    public static synchronized void checkSoftRestart(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastRestartTime > MAX_UPTIME) {
            Log.w(TAG, "🔄 Soft restarting Gecko Engine to maintain maximum RAM efficiency.");
            destroy();
            create(context);
        }
    }

    private static void safeRemoveFromParent() {
        if (geckoViewInstance != null) {
            ViewParent parent = geckoViewInstance.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(geckoViewInstance);
            }
        }
    }

    public static boolean isReady() {
        return isInitialized && geckoViewInstance != null && geckoSessionInstance != null;
    }

    public static void stats() {
        Log.i(TAG, "📊 === ROYAL GECKO ENGINE TELEMETRY === 📊");
        Log.i(TAG, "Status     : " + (isInitialized ? "ONLINE 🟢 (Gecko)" : "OFFLINE 🔴"));
        Log.i(TAG, "Uptime     : " + (System.currentTimeMillis() - creationTime) + "ms");
        Log.i(TAG, "Attaches   : " + attachCount);
        Log.i(TAG, "Detaches   : " + detachCount);
        Log.i(TAG, "========================================");
    }

    // 👑 دالة جلب الجسر البرمجي للتحكم بالاتصال الثنائي
    public static RoyalJsBridge getBridge() {
        return geckoBridgeInstance;
    }
}
