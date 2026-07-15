package com.store.app;

import android.content.Context;
import android.util.LruCache;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RoyalCacheManager {

    private static class CacheMeta {
        long expiry;
        String etag;
        String lastModified;
        long created;
    }

    private static final String TAG = "RoyalCacheManager";

    private static File cacheDir;

    // 👑 تحويل السقف إلى متغير ديناميكي يلتهم المساحة المتاحة بذكاء
    private static long MAX_DISK_CACHE;

    private static final int RAM_LIMIT = 20 * 1024 * 1024;
    private static final int RAM_THRESHOLD = 2 * 1024 * 1024;

    private static final LruCache<String, byte[]> memoryCache =
            new LruCache<String, byte[]>(RAM_LIMIT) {
                @Override
                protected int sizeOf(String key, byte[] value) {
                    return value.length;
                }
            };

    private static final Set<String> writingNow =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static long lastEviction = 0;

    private static final Set<String> EXT = new HashSet<>(Arrays.asList(
            ".png",".jpg",".jpeg",".webp",".avif",".gif",".ico",".svg",
            ".css",".js",".mjs",
            ".woff",".woff2",".ttf",".otf",
            ".mp4",".webm",".mp3",".wav",
            ".pdf",".doc",".docx"
    ));

    private static final Map<String, String> MIME = new HashMap<>();
    static {
        MIME.put(".webp","image/webp");
        MIME.put(".avif","image/avif");
        MIME.put(".woff2","font/woff2");
        MIME.put(".mjs","application/javascript");
        MIME.put(".svg","image/svg+xml");
        MIME.put(".html","text/html");
    }

    private RoyalCacheManager() {}

    public static void init(Context context) {
        if (cacheDir != null) return;

        // 👑 التوجيه نحو الذاكرة الخارجية إذا كانت متوفرة للحصول على مساحة أكبر
        File extCache = context.getExternalCacheDir();
        File targetDir = (extCache != null && extCache.getUsableSpace() > 200L * 1024 * 1024) 
                ? extCache : context.getCacheDir();
                
        cacheDir = new File(targetDir, "royal_cache_v4"); // ترقية الإصدار
        if (!cacheDir.exists()) cacheDir.mkdirs();

        // 👑 التهام المساحة باحترافية: 15% من مساحة الهاتف الفارغة! 
        // بحد أدنى 300 ميجابايت، وحد أقصى 2 جيجابايت (للمتاجر الضخمة).
        long usableSpace = cacheDir.getUsableSpace();
        MAX_DISK_CACHE = Math.max(300L * 1024 * 1024, 
                         Math.min((long)(usableSpace * 0.15), 2048L * 1024 * 1024));
                         
        Log.i(TAG, "🔥 Royal Cache Storage Allocated: " + (MAX_DISK_CACHE / (1024 * 1024)) + " MB");

        performLRUEviction();
    }

    // ==========================================
    // 🔥 INTERCEPT (L1 → L2)
    // ==========================================

    public static WebResourceResponse intercept(WebResourceRequest request) {

        long startTime = System.nanoTime();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        boolean success = true;

        try {
            if (cacheDir == null) return null;

            String url = request.getUrl().toString();
            if (!"GET".equalsIgnoreCase(request.getMethod())) return null;

            // 👑 حماية النخبة: منع تخزين أي طلب API خفي يتنكر كرابط عادي
            Map<String, String> requestHeaders = request.getRequestHeaders();
            if (requestHeaders != null) {
                String accept = requestHeaders.get("Accept");
                if (accept != null && (accept.contains("application/json") || accept.contains("text/event-stream"))) {
                    return null; // دعه يمر للإنترنت لأنه بيانات ديناميكية
                }
            }

            if (!isCacheable(url)) return null;

            maybeEvict();

            String key = md5(url);

            // ⚡ L1 RAM
            byte[] mem = memoryCache.get(key);
            if (mem != null) {
                return new WebResourceResponse(getMime(url), null,
                        new ByteArrayInputStream(mem));
            }

            // 💾 L2 Disk
            File file = new File(cacheDir, key);
            if (!file.exists()) return null;

            CacheMeta meta = loadMeta(key);

            if (meta == null) {
                file.delete();
                return null;
            }

            // 👑 تطبيق معمارية Stale-While-Revalidate (العرض الفوري والتحديث بالخلفية)
            if (System.currentTimeMillis() > meta.expiry) {
                Map<String, String> vHeaders = getValidationHeaders(url);
                // إرسال أمر للمحرك الشبكي ليقوم بالتحقق في الخلفية
                RoyalNetworkEngine.revalidateInBackground(url, vHeaders);
                // ⚡ لا نحذف الملف ولا نرجع null، بل سنستمر في الكود لنعرض النسخة الحالية فوراً!
            }

            try {
                // 🔥 SMALL → RAM
                if (file.length() < RAM_THRESHOLD) {

                    FileInputStream fis = new FileInputStream(file);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[8192];
                    int r;

                    while ((r = fis.read(buffer)) != -1) {
                        bos.write(buffer, 0, r);
                    }

                    fis.close();

                    byte[] data = bos.toByteArray();
                    memoryCache.put(key, data);

                    file.setLastModified(System.currentTimeMillis());

                    return new WebResourceResponse(getMime(url), null,
                            new ByteArrayInputStream(data));
                }

                // 🔥 LARGE → STREAM (بدون RAM)
                return new WebResourceResponse(
                        getMime(url),
                        null,
                        new BufferedInputStream(new FileInputStream(file))
                );

            } catch (Exception e) {
                return null;
            }

        } catch (Exception e) {
            success = false;
            return null;
        } finally {
            long latency = (System.nanoTime() - startTime) / 1_000_000;
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

            RoyalPanopticon.recordExecution(
                    "RoyalCacheManager",
                    latency,
                    success,
                    memoryUsed
            );
        }
    }

    // ==========================================
    // 💾 STORE
    // ==========================================

    public static void store(String url, InputStream inputStream, Map<String, List<String>> headers) {

        long startTime = System.nanoTime();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        boolean success = true;

        try {
            if (cacheDir == null) return;
            if (!isCacheable(url)) return;

            maybeEvict();

            String key = md5(url);

            // 🔒 atomic lock
            if (!writingNow.add(key)) return;

            try {
                CacheMeta meta = parseHeaders(url, headers);
                if (meta == null) return;

                File finalFile = new File(cacheDir, key);
                if (finalFile.exists() && finalFile.length() > 0) return;

                // 🛡️ الكتابة في ملف مؤقت أولاً (Atomic Write)
                File tmpFile = new File(cacheDir, key + ".tmp");
                FileOutputStream fos = new FileOutputStream(tmpFile);

                BufferedInputStream bis = new BufferedInputStream(inputStream);

                byte[] memBuffer = null;
                byte[] buffer = new byte[16384];
                int total = 0;
                int read;

                while ((read = bis.read(buffer)) != -1) {

                    fos.write(buffer, 0, read);
                    total += read;

                    if (total <= RAM_THRESHOLD) {
                        if (memBuffer == null) {
                            memBuffer = new byte[RAM_THRESHOLD];
                        }
                        System.arraycopy(buffer, 0, memBuffer, total - read, read);
                    }
                }

                fos.flush();
                fos.close();
                bis.close();

                // 🛡️ إنهاء عملية الكتابة الذرية بأمان
                if (tmpFile.length() == 0) {
                    tmpFile.delete();
                    return;
                } else {
                    // استبدال الملف القديم بالجديد في جزء من الثانية
                    tmpFile.renameTo(finalFile); 
                }

                // ⚡ RAM promotion
                if (memBuffer != null) {
                    byte[] exact = Arrays.copyOf(memBuffer, total);
                    memoryCache.put(key, exact);
                }

                saveMeta(key, meta);

                // 🔥 runtime eviction (خفيف)
                if (new Random().nextInt(20) == 0) {
                    performLRUEviction();
                }

            } catch (Exception ignored) {
            } finally {
                writingNow.remove(key);
            }

        } catch (Exception e) {
            success = false;
        } finally {
            long latency = (System.nanoTime() - startTime) / 1_000_000;
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

            RoyalPanopticon.recordExecution(
                    "RoyalCacheManager",
                    latency,
                    success,
                    memoryUsed
            );
        }
    }

    public static Map<String, String> getValidationHeaders(String url) {
        String key = md5(url);
        CacheMeta meta = loadMeta(key);

        if (meta == null) return null;

        Map<String, String> headers = new HashMap<>();

        if (meta.etag != null)
            headers.put("If-None-Match", meta.etag);

        if (meta.lastModified != null)
            headers.put("If-Modified-Since", meta.lastModified);

        return headers;
    }

    // ==========================================
    // 🧠 RULES
    // ==========================================

    private static boolean isCacheable(String url) {

        String clean = url.split("\\?")[0].toLowerCase();

        // APIs
        if (clean.contains("/api/")) return false;
        if (clean.contains("graphql")) return false;
        if (clean.contains("/wp-json/")) return false;
        if (clean.contains("/rest/")) return false;

        // Authentication
        if (clean.contains("login")) return false;
        if (clean.contains("logout")) return false;
        if (clean.contains("signin")) return false;
        if (clean.contains("signup")) return false;
        if (clean.contains("register")) return false;
        if (clean.contains("auth")) return false;
        if (clean.contains("oauth")) return false;

        // User Data
        if (clean.contains("/account")) return false;
        if (clean.contains("/profile")) return false;
        if (clean.contains("/user")) return false;
        if (clean.contains("/customer")) return false;
        if (clean.contains("/dashboard")) return false;

        // Commerce
        if (clean.contains("/cart")) return false;
        if (clean.contains("/checkout")) return false;
        if (clean.contains("/payment")) return false;
        if (clean.contains("/order")) return false;
        if (clean.contains("/invoice")) return false;

        // HTML - 👑 مسموح الآن للـ HTML بالمرور للقضاء على التجمد
        if (clean.endsWith(".php") || clean.endsWith(".jsp") || clean.endsWith(".asp") || clean.endsWith(".aspx")) return false;

        for (String e : EXT) {
            if (clean.endsWith(e)) {
                return true;
            }
        }
        
        // 👑 ذكاء اصطياد روابط المتاجر: 
        // المتاجر الحديثة (مثل site.com/product/123) ليس لها امتداد في النهاية.
        // إذا لم يكن الرابط يحتوي على امتداد ملف، فهو غالباً صفحة HTML هيكلية، سنسمح بتخزينها!
        if (!clean.matches(".*\\.[a-z0-9]{2,5}$")) {
            return true; 
        }

        return false;
    }

    private static long resolveTTL(String url) {

        String u = url.toLowerCase(Locale.US);

        if (u.endsWith(".js"))
            return 6L * 60 * 60 * 1000;

        if (u.endsWith(".css"))
            return 6L * 60 * 60 * 1000;

        if (u.endsWith(".woff"))
            return 30L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".woff2"))
            return 30L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".ttf"))
            return 30L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".otf"))
            return 30L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".png"))
            return 7L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".jpg"))
            return 7L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".jpeg"))
            return 7L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".webp"))
            return 7L * 24 * 60 * 60 * 1000;

        if (u.endsWith(".avif"))
            return 7L * 24 * 60 * 60 * 1000;

        // 👑 إذا كان الرابط هو صفحة HTML للمتجر، نعطيه TTL قصير جداً (5 دقائق)
        // هذا يضمن أن يفتح المتجر فوراً، ولكنه سيجبر المحرك على جلب النسخة الأحدث إذا تغيرت الأسعار.
        if (u.endsWith(".html") || !u.matches(".*\\.[a-z0-9]{2,5}$")) {
            return 5L * 60 * 1000; // 5 دقائق فقط
        }

        return 60L * 60 * 1000; // ساعة لباقي الملفات المجهولة
    }

    // ==========================================
    // 🧹 EVICTION
    // ==========================================

    private static void maybeEvict() {
        long now = System.currentTimeMillis();

        if (now - lastEviction > 5 * 60 * 1000) { // كل 5 دقائق
            lastEviction = now;
            performLRUEviction();
        }
    }

    private static void performLRUEviction() {

        long startTime = System.nanoTime();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        boolean success = true;

        try {

            File[] files = cacheDir.listFiles();
            if (files == null) return;

            long total = 0;
            for (File f : files) {

                if (f.getName().endsWith(".meta"))
                    continue;

                total += f.length();

            }

            if (total < MAX_DISK_CACHE) return;

            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    long diff = f1.lastModified() - f2.lastModified();
                    return (diff == 0) ? 0 : (diff < 0 ? -1 : 1);
                }
            });

            for (File f : files) {

                if (f.getName().endsWith(".meta"))
                    continue;

                total -= f.length();

                File meta = new File(f.getAbsolutePath() + ".meta");
                if (meta.exists()) meta.delete();

                f.delete();

                if (total < MAX_DISK_CACHE * 0.8) break;
            }

        } catch (Exception e) {
            success = false;
        } finally {
            long latency = (System.nanoTime() - startTime) / 1_000_000;
            long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

            RoyalPanopticon.recordExecution(
                    "RoyalCacheManager",
                    latency,
                    success,
                    memoryUsed
            );
        }
    }

    // ==========================================
    // 🔧 UTILS
    // ==========================================

    private static String getMime(String url) {
        String clean = url.toLowerCase().split("\\?")[0];

        for (Map.Entry<String, String> e : MIME.entrySet()) {
            if (clean.endsWith(e.getKey())) return e.getValue();
        }

        String sys = URLConnection.guessContentTypeFromName(clean);
        return sys != null ? sys : "application/octet-stream";
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();

        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static File metaFile(String key) {
        return new File(cacheDir, key + ".meta");
    }

    private static void saveMeta(String key, CacheMeta meta) {
        try (FileOutputStream fos = new FileOutputStream(metaFile(key))) {
            Properties p = new Properties();
            p.put("expiry", String.valueOf(meta.expiry));
            if (meta.etag != null) p.put("etag", meta.etag);
            if (meta.lastModified != null) p.put("lm", meta.lastModified);
            p.put("created", String.valueOf(System.currentTimeMillis()));
            p.store(fos, null);
        } catch (Exception ignored) {}
    }

    // 👑 تحديث وقت انتهاء الصلاحية فقط عند استلام 304 Not Modified
    public static void updateValidationMeta(String url, Map<String, List<String>> newHeaders) {
        String key = md5(url);
        CacheMeta oldMeta = loadMeta(key);
        
        if (oldMeta != null) {
            CacheMeta updatedMeta = parseHeaders(url, newHeaders);
            if (updatedMeta != null) {
                // دمج البيانات الجديدة مع القديمة
                oldMeta.expiry = updatedMeta.expiry;
                if (updatedMeta.etag != null) oldMeta.etag = updatedMeta.etag;
                if (updatedMeta.lastModified != null) oldMeta.lastModified = updatedMeta.lastModified;
                
                saveMeta(key, oldMeta);
            }
        }
    }

    private static CacheMeta loadMeta(String key) {
        File f = metaFile(key);
        if (!f.exists()) return null;

        try (FileInputStream fis = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(fis);

            CacheMeta m = new CacheMeta();
            m.expiry = Long.parseLong(p.getProperty("expiry", "0"));
            m.etag = p.getProperty("etag");
            m.lastModified = p.getProperty("lm");
            m.created = Long.parseLong(p.getProperty("created", "0"));
            return m;

        } catch (Exception e) {
            return null;
        }
    }

    private static CacheMeta parseHeaders(
            String url,
            Map<String, List<String>> headers) {

        CacheMeta meta = new CacheMeta();

        long now = System.currentTimeMillis();

        // 🔥 Cache-Control
        List<String> cc = headers.get("Cache-Control");
        if (cc != null) {
            String val = cc.get(0);
            String lower = val.toLowerCase(Locale.US);

            if (lower.contains("no-store")) return null;

            if (lower.contains("no-cache")) return null;

            if (lower.contains("private")) return null;

            if (lower.contains("must-revalidate")) return null;

            if (lower.contains("max-age")) {
                try {
                    String s = lower.split("max-age=")[1].split(",")[0];
                    long seconds = Long.parseLong(s);
                    meta.expiry = now + (seconds * 1000);
                } catch (Exception ignored) {}
            }
        }

        // fallback
        if (meta.expiry == 0) {
            meta.expiry = now + resolveTTL(url);
        }

        // 🔥 ETag
        List<String> et = headers.get("ETag");
        if (et != null) meta.etag = et.get(0);

        // 🔥 Last-Modified
        List<String> lm = headers.get("Last-Modified");
        if (lm != null) meta.lastModified = lm.get(0);

        return meta;
    }
                    }
