package com.store.app;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

/**
 * 👑 ROYAL WEBVIEW HOST (The True Immortal Gecko Core V5)
 * Architecture: Clean Separation of Concerns. 
 * - GeckoRuntime & GeckoSession are application-scoped singletons.
 * - GeckoView is strictly activity-scoped to respect Android lifecycle.
 */
public final class RoyalWebViewHost {

    private static final String TAG = "RoyalGeckoHost";

    private static GeckoRuntime geckoRuntimeInstance;
    private static GeckoSession geckoSessionInstance;
    private static RoyalJsBridge geckoBridgeInstance;
    private static boolean isInitialized = false;

    private RoyalWebViewHost() {}

    /**
     * 🚀 تهيئة المكونات الخالدة للمحرك (تُستدعى مرة واحدة في الـ Application أو أول Activity)
     */
    public static synchronized void init(@NonNull Context context) {
        if (isInitialized) return;

        // فرض التنفيذ على الـ UI Thread لضمان أمان المحرك
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("❌ FATAL: Gecko Runtime must be initialized on UI thread");
        }

        Log.i(TAG, "⚙️ Initializing Core GeckoRuntime and Session...");
        Context appContext = context.getApplicationContext();

        try {
            // 1️⃣ بناء إعدادات المحرك القياسية كما توصي Mozilla
            if (geckoRuntimeInstance == null) {
                GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                        .automaticCrashReporting(true)
                        .javaScriptEnabled(true)
                        .build();
                geckoRuntimeInstance = GeckoRuntime.create(appContext, settings);
            }

            // 2️⃣ بناء الجلسة المستقلة الثابتة وتجهيز إعداداتها
            if (geckoSessionInstance == null) {
                geckoSessionInstance = new GeckoSession();
                GeckoSessionSettings sessionSettings = geckoSessionInstance.getSettings();
                sessionSettings.setUseTrackingProtection(true); // حماية التتبع لتسريع التصفح
                sessionSettings.setAllowJavascript(true);       // تفعيل الجافا سكريبت
                sessionSettings.setDomStorageEnabled(true);     // تفعيل التخزين المحلي للموقع
            }

            // 3️⃣ ربط الجلسة بالمحرك الأساسي (تفتح مرة واحدة فقط)
            if (!geckoSessionInstance.isOpen()) {
                geckoSessionInstance.open(geckoRuntimeInstance);
            }

            // 🌉 إعداد وحقن الجسر البرمجي الملكي في الجلسة الثابتة
            geckoBridgeInstance = new RoyalJsBridge(geckoSessionInstance);
            geckoBridgeInstance.install();

            isInitialized = true;
            Log.i(TAG, "✅ Core Immortal Gecko Engine Components Initialized Successfully.");
        } catch (Exception e) {
            Log.e(TAG, "❌ FATAL: Failed to initialize Gecko Core Engines.", e);
        }
    }

    public static GeckoRuntime getRuntime() {
        return geckoRuntimeInstance;
    }

    public static GeckoSession getSession() {
        return geckoSessionInstance;
    }

    public static RoyalJsBridge getBridge() {
        return geckoBridgeInstance;
    }

    public static boolean isReady() {
        return isInitialized && geckoSessionInstance != null && geckoSessionInstance.isOpen();
    }
}
