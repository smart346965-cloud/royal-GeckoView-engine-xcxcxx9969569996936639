package com.store.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequestError;

/**
 * 👑 ROYAL GECKO ENGINE MANAGER (The Lifecycle Controller)
 * Handles Activity-scoped GeckoView configuration, bridging to the core Session,
 * Intent routing, and high-precision splash removal based on First Composite.
 */
public class WebEngineManager {

    private static final String TAG = "WebEngineManager";

    private final Context context;
    private final Activity activity;
    private final GeckoView geckoView;
    private final GeckoSession geckoSession;
    
    private final View splashOverlay;
    private final ProgressBar progressBar;
    private final Runnable markSplashRemoved;
    private final SplashStateChecker splashChecker;

    private String trustedScheme = null;
    private String trustedHost = null;
    private int trustedPort = -1;

    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable scrollFinishedRunnable = RoyalNetworkEngine::notifyScrollFinished;

    public interface SplashStateChecker {
        boolean isRemoved();
    }

    public WebEngineManager(@NonNull Context context,
                            @NonNull GeckoView geckoView,
                            @Nullable View splashOverlay,
                            @Nullable ProgressBar progressBar,
                            @NonNull Runnable markSplashRemoved,
                            @NonNull SplashStateChecker splashChecker) {
        this.context = context;
        this.geckoView = geckoView;
        this.splashOverlay = splashOverlay;
        this.progressBar = progressBar;
        this.markSplashRemoved = markSplashRemoved;
        this.splashChecker = splashChecker;
        
        this.activity = (context instanceof Activity) ? (Activity) context : null;
        
        // جلب الجلسة الخالدة الثابتة من الهوست
        this.geckoSession = RoyalWebViewHost.getSession();
    }

    /**
     * 🚀 ربط محرك العرض الخاص بالـ Activity الحالية مع الجلسة العامة
     */
    public void init() {
        if (geckoSession == null) {
            Log.e(TAG, "❌ Cannot initialize WebEngineManager, GeckoSession is null!");
            return;
        }

        configureViewSettings();
        attachDelegates();

        // ربط واجهة العرض الحالية بالجلسة (هنا يحدث السحر البرمجي الصحيح)
        geckoView.setSession(geckoSession);
    }

    private void configureViewSettings() {
        // إعدادات العرض الرسومية للـ View الحالي فقط
        geckoView.setBackgroundColor(Color.TRANSPARENT);
        geckoView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        geckoView.setWillNotDraw(false);

        // إعدادات التمرير
        geckoView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        geckoView.setHorizontalScrollBarEnabled(false);
        geckoView.setVerticalScrollBarEnabled(false);
    }

    private void attachDelegates() {
        // 🌉 ربط استدعاء إخفاء الـ Splash عبر الجسر
        if (RoyalWebViewHost.getBridge() != null) {
            RoyalWebViewHost.getBridge().setOnHideSplashCallback(this::removeSplashSmoothly);
        }

        // 🚀 PROGRESS DELEGATE
        geckoSession.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
                RoyalPanopticon.recordRequestSent();
            }

            @Override
            public void onPageStop(@NonNull GeckoSession session, boolean success) {
                RoyalPanopticon.recordNavigationComplete();
                RoyalNetworkEngine.notifyRenderIdle();
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
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setAlpha(1f);
                    }
                }
            }
        });

        // 🎨 CONTENT DELEGATE (إزالة الـ Splash هنا فقط تضمن عدم وجود شاشة بيضاء)
        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFirstComposite(@NonNull GeckoSession session) {
                RoyalPanopticon.recordFirstByteReceived();
                RoyalPanopticon.recordDomInteractive();
                RoyalNetworkEngine.notifyRenderStart();

                // التخلص الآمن والناعم من الشاشة الافتتاحية بعد التأكد من اكتمال رندرة أول بكسل مرئي
                removeSplashSmoothly();
            }
        });

        // 🚦 NAVIGATION DELEGATE (التوجيه الاحترافي وحماية النظام من الروابط الخارجية)
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
                String url = request.uri;
                Uri uri = Uri.parse(url);
                boolean isMainFrame = request.isDirectNavigation;

                if (trustedHost == null && isMainFrame && (url.startsWith("http://") || url.startsWith("https://"))) {
                    setTrustedOrigin(url);
                }

                if (isMainFrame && handleUriLogic(uri)) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY); // إيقاف التحميل داخلياً لأنه تم توجيهه خارجياً
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }

            @Nullable
            @Override
            public GeckoResult<String> onLoadError(@NonNull GeckoSession session, @Nullable String uri, @NonNull WebRequestError error) {
                Log.e(TAG, "☠️ Load Error: " + error.category);
                RoyalNetworkEngine.notifyRenderIdle();
                triggerOfflineProtection(uri);
                return GeckoResult.fromValue(null);
            }
        });

        // 📜 SCROLL DELEGATE
        geckoSession.setScrollDelegate(new GeckoSession.ScrollDelegate() {
            @Override
            public void onScrollChanged(@NonNull GeckoSession session, int scrollX, int scrollY) {
                RoyalNetworkEngine.notifyScroll(scrollY);
                scrollHandler.removeCallbacks(scrollFinishedRunnable);
                scrollHandler.postDelayed(scrollFinishedRunnable, 90);
            }
        });
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

    private void triggerOfflineProtection(String failingUrl) {
        if (failingUrl != null && !failingUrl.startsWith("resource://android/assets/")) {
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    String offline = "resource://android/assets/public/offline.html?origin=" + Uri.encode(failingUrl);
                    geckoSession.loadUri(offline);
                });
            }
        }
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

    private boolean handleUriLogic(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return true;

        // إرسال تطبيقات التواصل الخارجي كـ Intents لنظام أندرويد
        if (scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("whatsapp") || scheme.equals("intent")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {}
            return true;
        }

        if (scheme.equals("https") || scheme.equals("http")) {
            if (!NetworkMonitor.isInternetAvailable(context)) {
                triggerOfflineProtection(uri.toString());
                return true;
            }

            if (isSameOrigin(uri)) {
                return false; // الرابط من نفس المتجر الموثوق، افتحه داخلياً في GeckoView
            } else {
                try {
                    // أي رابط خارجي آخر يتم فتحه في متصفح خارجي آمن
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
                return true;
            }
        }
        return true;
    }

    /**
     * ⚠️ تفكيك الارتباط عند تدمير أو إيقاف الـ Activity لمنع تسريب الذاكرة
     */
    public void cleanup() {
        geckoView.setSession(null);
        geckoSession.setProgressDelegate(null);
        geckoSession.setContentDelegate(null);
        geckoSession.setNavigationDelegate(null);
        geckoSession.setScrollDelegate(null);
    }
}

