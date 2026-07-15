package com.store.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

public class WebEngineManager {

    private final Context context;
    private final android.app.Activity activity;
    private final WebView webView;
    private final View splashOverlay;
    private final android.widget.ProgressBar progressBar;

    private final Runnable markSplashRemoved;
    private final SplashStateChecker splashChecker;

    private String trustedScheme = null;
    private String trustedHost = null;
    private int trustedPort = -1;

    private final Runnable scrollFinishedRunnable =
            RoyalNetworkEngine::notifyScrollFinished;

    public interface SplashStateChecker {
        boolean isRemoved();
    }

    public WebEngineManager(Context context,
                            WebView webView,
                            View splashOverlay,
                            android.widget.ProgressBar progressBar,
                            Runnable markSplashRemoved,
                            SplashStateChecker splashChecker) {

        this.context = context;
        this.webView = webView;
        this.splashOverlay = splashOverlay;
        this.progressBar = progressBar;
        this.markSplashRemoved = markSplashRemoved;
        this.splashChecker = splashChecker;

        this.activity = (context instanceof android.app.Activity)
                ? (android.app.Activity) context
                : null;
    }

    public void init() {
        // 👑 1. حارس العودة الساخنة (Warm Resume Guard)
        if (RoyalWebViewHost.isReady() && webView.getUrl() != null && !webView.getUrl().equals("about:blank")) {
            android.util.Log.i("RoyalEngine", "🔥 Warm Resume Detected! Skipping Splash.");
            webView.setAlpha(1f);
            removeSplashInstantly();
            attachClients();
            return;
        }

        configureSettings();
        attachClients();

        // 👑 إرسال سرعة السكرول لمحرك الشبكة حتى تصبح قرارات الـ Prefetch أكثر ذكاءً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener(
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

                        RoyalNetworkEngine.notifyScroll(scrollY);

                        v.removeCallbacks(scrollFinishedRunnable);

                        v.postDelayed(scrollFinishedRunnable, 90);

                    });
        }
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
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setAlpha(0f);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWillNotDraw(false);
        webView.setOverScrollMode(WebView.OVER_SCROLL_IF_CONTENT_SCROLLS);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // 👑 فتح قواعد البيانات التخزينية العميقة (ضروري للـ Service Worker والـ IndexedDB)
        settings.setDatabaseEnabled(true);
        
        // 👑 السماح بالوصول للملفات لتتمكن نواة كروم من كتابة الـ Bytecode محلياً
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 👑 السماح للموارد المحلية بالاتصال ببعضها (مهم جداً لتخطي قيود الـ CORS داخل الكاش)
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.ALGORITHMIC_DARKENING)) {

            WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                    settings,
                    true
            );
        }

        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setSupportZoom(false);
    }

    private void attachClients() {
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // 👁️ [Panopticon Telemetry] تسجيل أول بايت واستقبال الطلب
                RoyalPanopticon.recordRequestSent();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 👁️ [Panopticon Telemetry] اكتمال عملية التحميل والرندرة الهيكلية
                RoyalPanopticon.recordNavigationComplete();

                // 1. حقن المحرك
                WebEnhancer.apply(view, context);
                
                // 2. 👑 ربط الجسر الملكي لإخفاء السبلاش فور وصول الإشارة
                // تعديل احترافي: نمرر كائن الـ Bridge المجهز بالـ Callback مباشرة للـ JavascriptInterface
                RoyalJsBridge bridge = new RoyalJsBridge(view);
                bridge.setOnHideSplashCallback(WebEngineManager.this::removeSplashSmoothly);
                view.addJavascriptInterface(bridge, "RoyalBridge");
                
                RoyalNetworkEngine.notifyRenderIdle();

                if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
                    WebViewCompat.postVisualStateCallback(
                            view,
                            System.currentTimeMillis(),
                            new WebViewCompat.VisualStateCallback() {
                                @Override
                                public void onComplete(long requestId) {
                                    RoyalNetworkEngine.notifyRenderIdle();
                                }
                            });
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                // 👁️ [Panopticon Telemetry] المتصفح يبدأ في عرض أول رسمة مرئية للمستخدم
                RoyalPanopticon.recordFirstByteReceived();
                RoyalPanopticon.recordDomInteractive();

                RoyalNetworkEngine.notifyRenderStart();

                if (!NetworkMonitor.isInternetAvailable(context)) {
                    return;
                }

                if (view.getAlpha() == 0f) {
                    view.animate().alpha(1f).setDuration(180).start();
                }

                if (trustedHost == null && url != null) {
                    setTrustedOrigin(url);
                }

                WebEnhancer.apply(view, context);
                syncStatusBarColor(view);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                android.util.Log.e("RoyalEngine", "☠️ FATAL: Chromium Renderer crashed! Auto-Recovery...");
                RoyalNetworkEngine.notifyRenderIdle();
                RoyalWebViewHost.destroy();
                if (activity != null) {
                    RoyalWebViewHost.create(activity.getApplicationContext());
                    activity.recreate();
                }
                return true;
            }

            private void triggerOfflineProtection(WebView view, String failingUrl) {
                if (failingUrl != null && !failingUrl.startsWith("file:///android_asset/")) {
                    RoyalNetworkEngine.notifyRenderIdle();
                    view.stopLoading();
                    view.clearHistory();
                    view.post(() -> {
                        String offline = "file:///android_asset/public/offline.html?origin=" + Uri.encode(failingUrl);
                        view.loadUrl(offline);
                    });
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    RoyalNetworkEngine.notifyRenderIdle();
                    triggerOfflineProtection(view, request.getUrl().toString());
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                RoyalNetworkEngine.notifyRenderIdle();
                triggerOfflineProtection(view, failingUrl);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return null;
                String url = request.getUrl().toString();

                if (url.endsWith("/nexus-service-worker.js")) {
                    try {
                        java.io.InputStream swStream = context.getAssets().open("public/js/nexus-service-worker.js");
                        java.util.Map<String, String> headers = new java.util.HashMap<>();
                        headers.put("Content-Type", "application/javascript");
                        headers.put("Service-Worker-Allowed", "/");
                        headers.put("Cache-Control", "no-cache"); 

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            return new WebResourceResponse("application/javascript", "UTF-8", 200, "OK", headers, swStream);
                        } else {
                            return new WebResourceResponse("application/javascript", "UTF-8", swStream);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RoyalEngine", "Failed to serve local Service Worker", e);
                    }
                }

                if (!NetworkMonitor.isInternetAvailable(context) && request.isForMainFrame()) {
                    return new WebResourceResponse("text/html", "UTF-8", null);
                }

                boolean isCoreResource = request.isForMainFrame() || url.contains(".js") || url.contains(".css");
                if (isCoreResource) {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
                }

                // 👁️ تتبع كفاءة الـ Network Interceptor الفرعي وتسجيل أدائه
                long startIntercept = System.currentTimeMillis();
                WebResourceResponse royalResponse = RoyalNetworkEngine.interceptRequest(request);
                long duration = System.currentTimeMillis() - startIntercept;
                
                // نرسل السجلات فوراً لمحرك الفحص لمعرفة سرعة استرجاع الكاش الملكي
                RoyalPanopticon.recordExecution("NetworkInterceptor", duration, true, 0);

                if (royalResponse != null) {
                    return royalResponse;
                }
                
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    Uri uri = request.getUrl();
                    String scheme = uri.getScheme();
                    if (scheme == null) return false;

                    if (scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("whatsapp") || scheme.equals("intent")) {
                        return handleUriLogic(uri, request.isForMainFrame());
                    }

                    if (scheme.equals("http") || scheme.equals("https")) {
                        if (!NetworkMonitor.isInternetAvailable(context)) {
                            String offline = "file:///android_asset/public/offline.html?origin=" + Uri.encode(uri.toString());
                            view.stopLoading();
                            view.loadUrl(offline);
                            return true;
                        }

                        if (!isSameOrigin(uri)) {
                            return handleUriLogic(uri, request.isForMainFrame());
                        }

                        // 👁️ [Panopticon Telemetry] تسجيل النقرة وتجهيز محرك قياس انتقال الصفحة
                        RoyalPanopticon.recordUserClick(uri.toString());
                        return false;
                    }
                }
                return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null) {
                    RoyalPanopticon.recordUserClick(url);
                    return handleUriLogic(Uri.parse(url), true);
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction(() -> progressBar.setVisibility(View.GONE))
                            .start();
                } else {
                    progressBar.setAlpha(1f);
                }
            }
        });
    }

    private void syncStatusBarColor(WebView view) {
        if (activity == null || activity.isFinishing()) return;

        if (!NetworkMonitor.isInternetAvailable(context))
            return;

        // 👑 قفل الحماية الملكي: إذا كنا في صفحة الأوفلاين المحلية، نفرض الألوان الشفافة والأيقونات الداكنة فوراً بدون تقييم مؤقت
        String currentUrl = view.getUrl();
        if (currentUrl != null && currentUrl.startsWith("file:///android_asset/")) {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            SystemUI.setDynamicIcons(activity.getWindow(), true); // true تعني أيقونات داكنة واضحة جداً فوق الخلفية البيضاء للأوفلاين
            return;
        }

        if (!view.isAttachedToWindow()) {
            return;
        }

        view.evaluateJavascript(
                "(function(){return window.getComputedStyle(document.body).backgroundColor;})();",
                value -> {
                    try {
                        if (value != null && value.contains("rgb")) {
                            String clean = value.replaceAll("[^0-9,]", "");
                            String[] parts = clean.split(",");
                            int r = Integer.parseInt(parts[0].trim());
                            int g = Integer.parseInt(parts[1].trim());
                            int b = Integer.parseInt(parts[2].trim());
                            int color = Color.rgb(r, g, b);

                            activity.getWindow().setStatusBarColor(color);
                            boolean isLight = SystemUI.isColorLight(color);
                            SystemUI.setDynamicIcons(activity.getWindow(), isLight);
                        }
                    } catch (Exception ignored) {}
                }
        );
    }

    private void setTrustedOrigin(String url) {
        Uri uri = Uri.parse(url);
        trustedScheme = uri.getScheme();
        trustedHost = uri.getHost();
        trustedPort = uri.getPort() == -1 ? (trustedScheme.equals("https") ? 443 : 80) : uri.getPort();
    }

    private boolean isSameOrigin(Uri uri) {
        if (trustedHost == null) return false;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) return false;

        int port = uri.getPort() == -1 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();
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
            if (isSameOrigin(uri)) {
                return false;
            } else {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }
        }
        return true;
    }
                }
