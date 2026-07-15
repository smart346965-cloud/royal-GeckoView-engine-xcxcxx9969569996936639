package com.store.app;

import android.app.Application;
import android.util.Log;

public class RoyalApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i("RoyalEngine", "🚀 Royal Application Ignite! Pre-warming WebView...");
        
        // 🌐 تشغيل رادار مراقبة الشبكة فوراً لخدمة الـ WebEngineManager والكاش
        NetworkMonitor.init(this);
        
        // 👁️ تشغيل عقل الفحص الملكي وبدء مراقبة خيط الواجهة الرئيسي (Main Looper)
        RoyalPanopticon.startAwareness();
        
        // ⚡ تسخين وخلق المحرك في الذاكرة بشكل صحيح متوافق مع بنية المعماريين
        RoyalWebViewHost.init(this);
    }

    @Override
    public void onTerminate() {
        RoyalPanopticon.stopAwareness();
        super.onTerminate();
    }
}
