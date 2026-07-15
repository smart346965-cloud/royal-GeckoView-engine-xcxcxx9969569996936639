package com.store.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

/**
 * 👑 MainActivity - النواة الأساسية لإدارة محرك الويب المخصص
 * تم تطهيرها بالكامل من مخلفات الـ TWA لتعمل بأقصى سرعة استجابة (Zero-friction)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RoyalMainActivity";
    private boolean splashRemoved = false;
    private WebEngineManager engineManager;
    private WebView activeWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🛡️ درع الوميض: مطابقة الخلفية مع لون السبلاش لمنع الوميض الأبيض الصارخ
        setTheme(R.style.AppTheme_NoSplash);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F3F4F6")));
        
        super.onCreate(savedInstanceState);

        // 🔍 تفعيل محرك الفحص والتشخيص الذكي (الآن سيغذي LogFox فوراً!)
        try {
            RoyalPanopticon.startAwareness();
            Log.i(TAG, "RoyalPanopticon Engine: Active and running in background.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RoyalPanopticon: " + e.getMessage());
        }

        // تفعيل أدوات تصحيح الويب التقنية عبر المتصفح
        WebView.setWebContentsDebuggingEnabled(true);

        // 1️⃣ استدعاء وتهيئة الويب فيو الخالد مباشرة بدون وسطاء
        if (!RoyalWebViewHost.isReady()) {
            RoyalWebViewHost.create(getApplicationContext());
        }
        activeWebView = RoyalWebViewHost.attach(this);

        // 2️⃣ تعيين المحرك الخالد كواجهة أساسية مباشرة (استجابة 0ms)
        setContentView(activeWebView);

        // 3️⃣ توجيه المحرك للهدف
        String targetUrl = "https://au.koala.com/"; 
        if (activeWebView.getUrl() == null || !activeWebView.getUrl().startsWith("http")) {
            activeWebView.loadUrl(targetUrl);
        }

        // 4️⃣ نظام التحكم بالرجوع المستقل نيتف (Native Back Press Handling)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeWebView != null && activeWebView.canGoBack()) {
                    activeWebView.post(() -> activeWebView.goBack());
                } else {
                    // إرسال التطبيق للخلفية بدلاً من إغلاقه بالكامل للحفاظ على الويب فيو ساخناً بالذاكرة
                    moveTaskToBack(true);
                }
            }
        });

        // 5️⃣ الحصانة البصرية وتخصيص شريط النظام بالكامل
        SystemUI.applyKingMode(this, activeWebView);
        SystemUI.setDynamicIcons(this.getWindow(), true);

        // 6️⃣ بناء وتجهيز طبقة شاشة التحميل (Splash Screen Overlay)
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

        // 👑 المزامنة المطلقة: ربط السبلاش نيتف بإشارة اكتمال الرندر القادمة من الويب (JS Bridge)
        if (RoyalWebViewHost.getBridge() != null) {
            RoyalWebViewHost.getBridge().setOnHideSplashCallback(() -> {
                if (!splashRemoved) {
                    engineManager.removeSplashSmoothly();
                }
            });
        }

        // Fail-safe: إخفاء الشاشة تلقائياً في حالة حدوث تأخير خارق للعادة لحماية الـ UX
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!splashRemoved && activeWebView != null) {
                Log.w(TAG, "Fail-safe: Forced reveal after timeout");
                splashOverlay.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                splashRemoved = true;
            }
        }, 100); // القيمة 100ms للمزامنة اللحظية الفائقة، ويمكن زيادتها حسب الحاجة
    }

    @Override
    protected void onPause() {
        super.onPause();
        // إيقاف مؤقت للعمليات الرسومية غير النشطة في الخلفية للحفاظ على طاقة الجهاز
        if (activeWebView != null) {
            activeWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // استئناف العمليات الرسومية والـ JavaScript فور عودة المستخدم للتطبيق
        if (activeWebView != null) {
            activeWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        // 🛡️ فك الارتباط الجراحي: فصل الويب فيو بأمان تام لتجنب أخطاء تدمير الـ Buffers الرسومية
        if (activeWebView != null) {
            // إيقاف الفيديوهات أو الصوتيات التي قد تعمل في الخلفية لحظة الإغلاق
            activeWebView.loadUrl("about:blank");
        }
        RoyalWebViewHost.detach();
        super.onDestroy();
    }
}
