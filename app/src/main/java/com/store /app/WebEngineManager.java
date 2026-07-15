package com.store.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequestError;

/**
 * =========================================================
 * 👑 ROYAL GECKO ENGINE MANAGER (The Core Controller)
 * =========================================================
 * Architecture: GeckoView Native Integration, Splash Synchronization,
 * Scroll Telemetry, Offline Recovery, and Intent Routing.
 */
public class WebEngineManager {

    private final Context context;
    private final android.app.Activity activity;
    private final GeckoView geckoView;
    private final GeckoSession geckoSession;
    private final View splashOverlay;
    private final android.widget.ProgressBar progressBar;

    private final Runnable markSplashRemoved;
    private final SplashStateChecker splashChecker;

    private String trustedScheme = null;
    private String trustedHost = null;
    private int trustedPort = -1;

    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable scrollFinishedRunnable =
            RoyalNetworkEngine::notifyScrollFinished;

    public interface SplashStateChecker {
        boolean isRemoved();
    }

    public WebEngineManager(Context context,
                            GeckoView geckoView,
                            View splashOverlay,
                            android.widget.ProgressBar progressBar,
                            Runnable markSplashRemoved,
                            SplashStateChecker splashChecker) {

        this.context = context;
        this.geckoView = geckoView;
        this.splashOverlay = splashOverlay;
        this.progressBar = progressBar;
        this.markSplashRemoved = markSplashRemoved;
        this.splashChecker = splashChecker;

        // استدعاء الجلسة من الخادم الخالد الذي صنعناه في الملف الأول
        this.geckoSession = RoyalWebViewHost.getSession();

        this.activity = (context instanceof android.app.Activity)
                ? (android.app.Activity) context
                : null;
    }

    public void init() {
        // 👑 1. حارس العودة الساخنة (Warm Resume Guard)
        // التحقق من أن المحرك جاهز وأنه تم تحميل رابط حقيقي مسبقاً
        if (RoyalWebViewHost.isReady() && geckoSession.isOpen()) {
            android.util.Log.i("RoyalEngine", "🔥 Warm Resume Detected! Skipping Splash.");
            geckoView.setAlpha(1f);
            removeSplashInstantly();
            attachDelegates();
            return;
        }

        configureSettings();
        attachDelegates();
    }

    private void removeSplashInstantly() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (splashOverlay != null && splashOverlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) splashOverlay.getParent()).removeView(splashOverlay);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            markSplashRemoved.run();
            RoyalNetworkEngine.notifyRenderIdle();
        });
    }

    public void removeSplashSmoothly() {
        if (activity == null || splashChecker.isRemoved()) return;
        activity.runOnUiThread(() -> {
            if (splashOverlay != null) {
                splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(this::removeSplashInstantly)
                        .start();
            }
        });
    }

    private void configureSettings() {
        geckoView.setBackgroundColor(Color.TRANSPARENT);
        geckoView.setAlpha(0f);
        geckoView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        geckoView.setWillNotDraw(false);

        // إعدادات التمرير لـ GeckoView
        geckoView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        geckoView.setHorizontalScrollBarEnabled(false);
        geckoView.setVerticalScrollBarEnabled(false);

        // ملاحظة: إعدادات DOM و JS وقواعد البيانات تم تفعيلها مسبقاً باحترافية
        // داخل ملف RoyalWebViewHost أثناء إنشاء الـ GeckoSession
    }

    private void attachDelegates() {
        
        // 👑 1. ربط الجسر الملكي لإخفاء السبلاش (استدعاء من الخادم الخالد)
        if (RoyalWebViewHost.getBridge() != null) {
            RoyalWebViewHost.getBridge().setOnHideSplashCallback(this::removeSplashSmoothly);
        }

        // =======================================================
        // 🚀 PROGRESS DELEGATE (بديل WebChromeClient)
        // =======================================================
        geckoSession.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                // 👁️ [Panopticon Telemetry] تسجيل أول بايت واستقبال الطلب
                RoyalPanopticon.recordRequestSent();
            }

            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                // 👁️ [Panopticon Telemetry] اكتمال عملية التحميل والرندرة
                RoyalPanopticon.recordNavigationComplete();

                // حقن المحرك الخاص بك
                WebEnhancer.apply(geckoView, context);
                
                RoyalNetworkEngine.notifyRenderIdle();
                syncStatusBarColor(session);
            }

            @Override
            public void onProgressChange(@NonNull GeckoSession session, int progress) {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                    if (progress == 100) {
                        progressBar.animate()
                                .alpha(0f)
                                .setDuration(150)
                                .withEndAction(() -> progressBar.setVisibility(View.GONE))
                                .start();
                    } else {
                        progressBar.setAlpha(1f);
                    }
                }
            }
        });

        // =======================================================
        // 🎨 CONTENT DELEGATE (لرصد اللحظة المرئية بدقة فائقة)
        // =======================================================
        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFirstComposite(@NonNull GeckoSession session) {
                // 👁️ يعادل onPageCommitVisible: المتصفح يعرض أول رسمة
                RoyalPanopticon.recordFirstByteReceived();
                RoyalPanopticon.recordDomInteractive();

                RoyalNetworkEngine.notifyRenderStart();

                if (!NetworkMonitor.isInternetAvailable(context)) {
                    return;
                }

                // إظهار المحرك بنعومة بعد بدء الرندرة
                activity.runOnUiThread(() -> {
                    if (geckoView.getAlpha() == 0f) {
                        geckoView.animate().alpha(1f).setDuration(180).start();
                    }
                });

                WebEnhancer.apply(geckoView, context);
            }
        });

        // =======================================================
        // 🚦 NAVIGATION DELEGATE (بديل WebViewClient - للتحكم بالروابط)
        // =======================================================
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
                String url = request.uri;
                Uri uri = Uri.parse(url);
                boolean isMainFrame = request.isDirectNavigation;

                // تحديث النطاق الموثوق
                if (trustedHost == null && isMainFrame && (url.startsWith("http://") || url.startsWith("https://"))) {
                    setTrustedOrigin(url);
                }

                if (isMainFrame) {
                    // اعتراض الطلبات المخصصة (Service Worker) - توجيه الطلب لنواة الشبكة
                    if (url.endsWith("/nexus-service-worker.js")) {
                        RoyalNetworkEngine.interceptRequest(null); // تسجيل وهمي لمحركك
                        // GeckoView يتعامل مع الـ Service Worker داخلياً بشكل أفضل
                    }

                    if (handleUriLogic(uri, isMainFrame)) {
                        return GeckoResult.fromValue(AllowOrDeny.DENY); // إلغاء التحميل الداخلي
                    }
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW); // السماح بالتحميل
            }

            @Nullable
            @Override
            public GeckoResult<String> onLoadError(@NonNull GeckoSession session, @Nullable String uri, @NonNull WebRequestError error) {
                android.util.Log.e("RoyalEngine", "☠️ Load Error: " + error.category);
                RoyalNetworkEngine.notifyRenderIdle();
                triggerOfflineProtection(uri);
                return GeckoResult.fromValue(null);
            }
        });

        // =======================================================
        // 📜 SCROLL DELEGATE (لإرسال بيانات السكرول للـ NetworkEngine)
        // =======================================================
        geckoSession.setScrollDelegate(new GeckoSession.ScrollDelegate() {
            @Override
            public void onScrollChanged(@NonNull GeckoSession session, int scrollX, int scrollY) {
                RoyalNetworkEngine.notifyScroll(scrollY);
                scrollHandler.removeCallbacks(scrollFinishedRunnable);
                scrollHandler.postDelayed(scrollFinishedRunnable, 90);
            }
        });
    }

    private void triggerOfflineProtection(String failingUrl) {
        if (failingUrl != null && !failingUrl.startsWith("file:///android_asset/")) {
            activity.runOnUiThread(() -> {
                String offline = "resource://android/assets/public/offline.html?origin=" + Uri.encode(failingUrl);
                geckoSession.loadUri(offline);
            });
        }
    }

    private void syncStatusBarColor(GeckoSession session) {
        if (activity == null || activity.isFinishing()) return;

        if (!NetworkMonitor.isInternetAvailable(context)) return;

        // استخراج لون خلفية الموقع باستخدام GeckoScript
        session.evaluateScript("(function(){return window.getComputedStyle(document.body).backgroundColor;})();")
                .then(result -> {
                    if (result != null) {
                        String value = result.toString();
                        if (value.contains("rgb")) {
                            try {
                                String clean = value.replaceAll("[^0-9,]", "");
                                String[] parts = clean.split(",");
                                int r = Integer.parseInt(parts[0].trim());
                                int g = Integer.parseInt(parts[1].trim());
                                int b = Integer.parseInt(parts[2].trim());
                                int color = Color.rgb(r, g, b);

                                activity.runOnUiThread(() -> {
                                    activity.getWindow().setStatusBarColor(color);
                                    boolean isLight = SystemUI.isColorLight(color);
                                    SystemUI.setDynamicIcons(activity.getWindow(), isLight);
                                });
                            } catch (Exception ignored) {}
                        }
                    }
                    return null;
                });
    }

    private void setTrustedOrigin(String url) {
        Uri uri = Uri.parse(url);
        trustedScheme = uri.getScheme();
        trustedHost = uri.getHost();
        trustedPort = uri.getPort() == -1 ? ("https".equals(trustedScheme) ? 443 : 80) : uri.getPort();
    }

    private boolean isSameOrigin(Uri uri) {
        if (trustedHost == null) return false;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) return false;

        int port = uri.getPort() == -1 ? ("https".equals(scheme) ? 443 : 80) : uri.getPort();
        return scheme.equals(trustedScheme) && host.equalsIgnoreCase(trustedHost) && port == trustedPort;
    }

    private boolean handleUriLogic(Uri uri, boolean isMainFrame) {
        if (!isMainFrame) return false;
        String scheme = uri.getScheme();
        if (scheme == null) return true;

        if (scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("whatsapp") || scheme.equals("intent")) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {}
            return true;
        }

        if (scheme.equals("https") || scheme.equals("http")) {
            if (!NetworkMonitor.isInternetAvailable(context)) {
                triggerOfflineProtection(uri.toString());
                return true;
            }

            if (isSameOrigin(uri)) {
                return false; // اسمح لـ GeckoView بفتح الرابط داخلياً
            } else {
                try {
                    // الروابط الخارجية تفتح في متصفح النظام
                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }
        }
        return true;
    }
}

