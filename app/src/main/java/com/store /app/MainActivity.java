package com.store.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.HistoryDelegate.HistoryList;

/**
👑 MainActivity - النواة الأساسية لإدارة محرك الويب المخصص (GeckoView Powered)
تم تطهيرها بالكامل لتعمل بأقصى سرعة استجابة (Zero-friction) مع استقرار تام.
*/
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RoyalMainActivity";
    private boolean splashRemoved = false;
    private WebEngineManager engineManager;
    private GeckoView activeWebView; // تم ترقيتها إلى GeckoView
    private GeckoSession activeSession;

    // لمتابعة حالة الرجوع للخلف بدقة داخل الـ History
    private boolean canGoBackValue = false;
    
    // لتتبع الرابط النشط حالياً بدلاً من getActiveUri المفقود في هذه النسخة
    private String currentUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🛡️ درع الوميض: مطابقة الخلفية لمنع الوميض الأبيض الصارخ
        setTheme(R.style.AppTheme_NoSplash);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F3F4F6")));

        super.onCreate(savedInstanceState);  

        // 🔍 تفعيل محرك الفحص والتشخيص الذكي  
        try {  
            RoyalPanopticon.startAwareness();  
            Log.i(TAG, "RoyalPanopticon Engine: Active and running in background.");  
        } catch (Exception e) {  
            Log.e(TAG, "Failed to initialize RoyalPanopticon: " + e.getMessage());  
        }  

        // 1️⃣ استدعاء وتهيئة خادم Gecko الخالد مباشرة  
        if (!RoyalWebViewHost.isReady()) {  
            RoyalWebViewHost.create(getApplicationContext());  
        }  

        activeWebView = RoyalWebViewHost.attach(this);  
        activeSession = RoyalWebViewHost.getSession();  

        // ربط الجسر والـ PromptDelegate بـ Gecko  
        if (activeSession != null) {  
            activeSession.setPromptDelegate(RoyalWebViewHost.getBridge());  

            // 👑 مراقبة التاريخ لتمكين التراجع وتحديث الرابط النشط باحترافية وبدون تعارض مع مدير المحرك
            activeSession.setHistoryDelegate(new GeckoSession.HistoryDelegate() {  
                @Override  
                public void onHistoryStateChange(@NonNull GeckoSession session, @NonNull HistoryList historyList) {  
                    canGoBackValue = historyList.getCurrentIndex() > 0;  
                    
                    // استخراج الرابط الحالي بأمان من سجل التاريخ لتجنب مشاكل توافق إصدارات المكتبة المتغيرة
                    if (historyList.getCurrentIndex() >= 0 && historyList.getCurrentIndex() < historyList.size()) {
                        currentUrl = historyList.get(historyList.getCurrentIndex()).getUri();
                    }
                }  
            });  
        }  

        // 2️⃣ تعيين المحرك الخالد كواجهة أساسية مباشرة (استجابة 0ms)  
        setContentView(activeWebView);  

        // 3️⃣ توجيه المحرك للهدف  
        String targetUrl = "https://au.koala.com/";   
        if (activeSession != null && (currentUrl == null || !currentUrl.startsWith("http"))) {  
            activeSession.loadUri(targetUrl);  
        }  

        // 4️⃣ نظام التحكم بالرجوع المستقل نيتف (Native Back Press Handling)  
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {  
            @Override  
            public void handleOnBackPressed() {  
                if (activeSession != null && canGoBackValue) {  
                    activeSession.goBack();  
                } else {  
                    // إرسال التطبيق للخلفية للحفاظ على المتصفح ساخناً بالرام  
                    moveTaskToBack(true);  
                }  
            }  
        });  

        // 5️⃣ الحصانة البصرية وتخصيص شريط النظام بالكامل  
        // نمرر WebView مؤقت لإرضاء ميثود الـ SystemUI المتوقع لـ WebView وتجنب خطأ التعارض البرمجي
        SystemUI.applyKingMode(this, new android.webkit.WebView(this));  
        SystemUI.setDynamicIcons(this.getWindow(), true);  

        // 6️⃣ بناء وتجهيز طبقة شاشة التحميل  
        setupSplashScreen();
    }

    private void setupSplashScreen() {
        // شاشة التحميل البرمجية (Splash Overlay)
        final View splashOverlay = new View(this);
        splashOverlay.setBackgroundColor(Color.parseColor("#F3F4F6"));
        splashOverlay.setAlpha(1f);

        addContentView(splashOverlay, new ViewGroup.LayoutParams(  
                ViewGroup.LayoutParams.MATCH_PARENT,  
                ViewGroup.LayoutParams.MATCH_PARENT));  

        // شريط التقدم النحيف الأنيق (Progress Bar)  
        final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);  
        progressBar.setMax(100);  
        progressBar.setProgress(0);  
        progressBar.setIndeterminate(false);  

        ViewGroup.LayoutParams progressParams = new ViewGroup.LayoutParams(  
                ViewGroup.LayoutParams.MATCH_PARENT, 6);  
        progressBar.setLayoutParams(progressParams);  
        progressBar.setScaleY(0.6f);  

        addContentView(progressBar, progressParams);  

        // 7️⃣ ربط المحرك بمدير المحتوى ومراقبة التحميل  
        engineManager = new WebEngineManager(  
                this,  
                activeWebView,  
                splashOverlay,  
                progressBar,  
                () -> splashRemoved = true,  
                () -> splashRemoved  
        );  
        engineManager.init();  

        // 👑 المزامنة المطلقة: ربط السبلاش بإشارة اكتمال الرندر  
        if (RoyalWebViewHost.getBridge() != null) {  
            RoyalWebViewHost.getBridge().setOnHideSplashCallback(() -> {  
                if (!splashRemoved) {  
                    engineManager.removeSplashSmoothly();  
                }  
            });  
        }  

        // Fail-safe: إخفاء شاشة البدء في حال حدوث تأخير مفاجئ لحماية تجربة العميل  
        new Handler(Looper.getMainLooper()).postDelayed(() -> {  
            if (!splashRemoved && activeWebView != null) {  
                Log.w(TAG, "Fail-safe: Forced reveal after timeout");  
                splashOverlay.setVisibility(View.GONE);  
                progressBar.setVisibility(View.GONE);  
                splashRemoved = true;  
            }  
        }, 100); // بقيمة 100ms للمزامنة اللحظية الساخنة
    }

    @Override
    protected void onPause() {
        super.onPause();
        // يتم تجميد العمليات غير الضرورية بأمان عبر استدعاء الـ detach التلقائي للويب هوست
        RoyalWebViewHost.detach();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // استئناف الرندرة والـ JavaScript فور عودة العميل للتطبيق
        if (activeWebView != null) {
            RoyalWebViewHost.attach(this);
        }
    }

    @Override
    protected void onDestroy() {
        // 🛡️ فك الارتباط الجراحي لمنع تسريب موارد النظام والبطارية
        if (activeSession != null) {
            activeSession.loadUri("about:blank");
        }
        RoyalWebViewHost.destroy();
        super.onDestroy();
    }
}
