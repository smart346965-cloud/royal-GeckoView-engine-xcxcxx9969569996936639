package com.store.app;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * =========================================================
 * 👑 ROYAL HYBRID ENGINE (V2 - Runtime Controller)
 * =========================================================
 * - Process Priority Enforcement (OOM Protection)
 * - Offscreen Pre-Rasterization
 * - Chromium Native Cache Delegation
 * - Capacitor-Safe (Zero Plugin Breakage)
 */
public final class RoyalHybridEngine {

    private static final String TAG = "RoyalHybridEngine";
    private static boolean isEnginePrimed = false;

    // 🔒 منع إنشاء كائنات (Singleton Utility)
    private RoyalHybridEngine() {}

    /**
     * 🚀 نقطة الإشعال (Prime The Engine)
     * يتم استدعاؤها مرة واحدة فقط لتسليح الـ WebView.
     */
    public static void prime(WebView webView, Context context) {
        if (webView == null || isEnginePrimed) return;

        Log.i(TAG, "⚙️ Priming Royal Hybrid Engine V2 (Runtime Controller)...");

        WebSettings settings = webView.getSettings();

        // ==========================================
        // 1️⃣ Process Survival (حماية العملية من القتل)
        // ==========================================
        // إخبار نظام أندرويد أن هذا الويب فيو "حرج جداً" ولا يجب قتله لتوفير الـ RAM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }

        // ==========================================
        // 2️⃣ Rendering Discipline (الرسم المسبق)
        // ==========================================
        // إجبار محرك Chromium على رسم الصفحة في الذاكرة حتى لو لم تكن ظاهرة على الشاشة بالكامل
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.setOffscreenPreRaster(true);
        }

        // ==========================================
        // 3️⃣ Chromium Native Cache (تفويض الكاش للمحرك)
        // ==========================================
        // تركنا Chromium يدير الشبكة بذكائه الخاص (ETag, Disk, Memory) كما نصح الخبير
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // 💥 ربط الكاش بالجلسة: تفعيل الكوكيز لربط التخزين المؤقت بجلسة المستخدم
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // ==========================================
        // 4️⃣ SPA Stability & Security (استقرار وأمان)
        // ==========================================
        settings.setJavaScriptEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // السماح بالصور
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);

        // 🛡️ درع الوميض الأول: إجبار الويب فيو على اللون الأبيض (أو لون متجرك) بدلاً من الشفاف الذي يسبب سواداً
        webView.setBackgroundColor(android.graphics.Color.WHITE);

        // 🛍️ إعدادات التجارة الإلكترونية (E-Commerce Native Feel)
        // 1. السماح بتشغيل الفيديوهات الترويجية تلقائياً بدون تدخل المستخدم
        settings.setMediaPlaybackRequiresUserGesture(false);

        isEnginePrimed = true;
        Log.i(TAG, "✅ Royal Hybrid Engine V2: Process Priority & Pre-Raster Primed.");
    }

    /**
     * 🔄 إعادة ضبط المحرك (تُستدعى فقط عند الـ Soft Restart)
     */
    public static void reset() {
        isEnginePrimed = false;
    }
}
