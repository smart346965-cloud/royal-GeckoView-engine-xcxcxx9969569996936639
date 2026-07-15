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

import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSession.HistoryDelegate.HistoryList;

/**
 👑 MainActivity - النواة الأساسية لإدارة محرك الويب المخصص (GeckoView Powered)
 تم تطهيرها ومزامنتها برمجياً بالكامل مع بنية الـ Core v5 الاحترافية.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RoyalMainActivity";
    private boolean splashRemoved = false;
    private WebEngineManager engineManager;
    private GeckoView activeWebView; 
    private GeckoSession activeSession;

    private boolean canGoBackValue = false;
    private String currentUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🛡️ درع الوميض
        setTheme(R.style.AppTheme_NoSplash);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F3F4F6")));

        super.onCreate(savedInstanceState);  

        // 🔍 تفعيل محرك الفحص والتشخيص الذكي  
        try {  
            RoyalPanopticon.startAwareness();  
        } catch (Exception e) {  
            Log.e(TAG, "Failed to initialize RoyalPanopticon: " + e.getMessage());  
        }  

        // 1️⃣ التأكد من تهيئة المحرك الأساسي
        RoyalWebViewHost.init(getApplicationContext());  

        // 2️⃣ بناء واجهة العرض (GeckoView) الخاصة بهذه الـ Activity فقط لمنع تسريب الذاكرة
        activeWebView = new GeckoView(this);
        activeSession = RoyalWebViewHost.getSession();  

        // ربط الجسر والـ PromptDelegate بـ Gecko  
        if (activeSession != null) {  
            activeSession.setPromptDelegate(RoyalWebViewHost.getBridge());  

            // 👑 مراقبة التاريخ لتمكين التراجع 
            activeSession.setHistoryDelegate(new GeckoSession.HistoryDelegate() {  
                @Override  
                public void onHistoryStateChange(@NonNull GeckoSession session, @NonNull HistoryList historyList) {  
                    canGoBackValue = historyList.getCurrentIndex() > 0;  
                    if (historyList.getCurrentIndex() >= 0 && historyList.getCurrentIndex() < historyList.size()) {
                        currentUrl = historyList.get(historyList.getCurrentIndex()).getUri();
                    }
                }  
            });  
        }  

        // 3️⃣ تعيين محرك العرض كواجهة أساسية مباشرة للـ Activity الحالية
        setContentView(activeWebView);  

        // 4️⃣ تحميل الرابط لأول مرة
        if (savedInstanceState == null && activeSession != null) {
            String targetUrl = "https://au.koala.com/";  
            if (currentUrl == null || !currentUrl.startsWith("http")) {  
                activeSession.loadUri(targetUrl);  
            }  
        }  

        // 5️⃣ نظام التحكم بالرجوع المستقل نيتف (Native Back Press Handling)  
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {  
            @Override  
            public void handleOnBackPressed() {  
                if (activeSession != null && canGoBackValue) {  
                    activeSession.goBack();  
                } else {  
                    moveTaskToBack(true);  
                }  
            }  
        });  

        // 6️⃣ الحصانة البصرية وتخصيص شريط النظام
        SystemUI.applyKingMode(this, new android.webkit.WebView(this));  
        SystemUI.setDynamicIcons(this.getWindow(), true);  

        // 7️⃣ بناء وتجهيز طبقة شاشة التحميل  
        setupSplashScreen();
    }

    private void setupSplashScreen() {
        final View splashOverlay = new View(this);
        splashOverlay.setBackgroundColor(Color.parseColor("#F3F4F6"));
        splashOverlay.setAlpha(1f);

        addContentView(splashOverlay, new ViewGroup.LayoutParams(  
                ViewGroup.LayoutParams.MATCH_PARENT,  
                ViewGroup.LayoutParams.MATCH_PARENT));  

        final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);  
        progressBar.setMax(100);  
        progressBar.setProgress(0);  
        progressBar.setIndeterminate(false);  

        ViewGroup.LayoutParams progressParams = new ViewGroup.LayoutParams(  
                ViewGroup.LayoutParams.MATCH_PARENT, 6);  
        progressBar.setLayoutParams(progressParams);  
        progressBar.setScaleY(0.6f);  

        addContentView(progressBar, progressParams);  

        // 8️⃣ ربط المحرك بمدير المحتوى وبدء الربط مع الجلسة العامة
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

        // Fail-safe حماية تجربة العميل
        new Handler(Looper.getMainLooper()).postDelayed(() -> {  
            if (!splashRemoved) {  
                splashOverlay.setVisibility(View.GONE);  
                progressBar.setVisibility(View.GONE);  
                splashRemoved = true;  
            }  
        }, 100); 
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // 🛡️ تفكيك الارتباط لمنع أي تسريب للذاكرة عند إغلاق الأكتيفيتي
        if (engineManager != null) {
            engineManager.cleanup(); 
        }
        super.onDestroy();
    }
}
