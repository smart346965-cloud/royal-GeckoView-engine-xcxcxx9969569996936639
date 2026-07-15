/**
 * =========================================================
 * ⚡ ROYAL INTERACTION ENGINE (V4 - The Instant Silk Touch)
 * =========================================================
 * Architecture: GPU Offloading, Passive Listeners, rAF Batching.
 * Updates: Instant Click (Slop Detection), Visual Feedback, BFCache Sanitizer.
 */

(function () {
    'use strict';

    // =========================================================
    // 🎨 1. CSS ACCELERATION & VISUAL FEEDBACK
    // =========================================================
    function injectHardwareAcceleration() {
        if (document.getElementById('royal-interaction-styles')) return;

        const style = document.createElement("style");
        style.id = 'royal-interaction-styles';
        style.textContent = `
            * { -webkit-tap-highlight-color: transparent !important; }
            a, button,[role="button"], input, select, textarea { touch-action: manipulation !important; }
            
            /* فقط العناصر غير الحساسة */
            img[loading="lazy"] {
                content-visibility: auto;
            }

            /* تسريع GPU بدون كسر التحميل */
            img {
                transform: translateZ(0);
                backface-visibility: hidden;
            }
            
            video, canvas, svg { transform: translateZ(0); backface-visibility: hidden; }
            
            /* فقط العناصر الثقيلة جداً */
            body.royal-is-scrolling iframe {
                pointer-events: none !important;
            }
            body.royal-is-scrolling { will-change: scroll-position; }

            /* 👑 الومضة البصرية الناعمة عند اللمس (Visual Feedback) */
            .royal-tap-active {
                opacity: 0.6 !important;
                transition: none !important;
            }
            .royal-tap-release {
                transition: opacity 0.3s ease-out !important;
            }
        `;
        document.head.appendChild(style);
    }

    // =========================================================
    // 👆 2. TAP ENGINE (محرك النقر اللحظي الذكي)
    // =========================================================
    const TapEngine = {
        init: function () {
            let startX = 0;
            let startY = 0;
            let isScrolling = false;
            let activeLink = null;

            // 1. عند اللمس: ومضة بصرية + تسخين
            document.addEventListener("touchstart", (e) => {
                if (e.touches.length === 0) return;

                startX = e.touches[0].clientX;
                startY = e.touches[0].clientY;
                isScrolling = false;

                const link = e.target.closest("a[href]");
                if (link && link.href) {
                    try {
                        const url = link.href;
                        if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("javascript:") || url.startsWith("#")) return;
                        if (new URL(url).origin !== window.location.origin) return;

                        activeLink = link;
                        
                        // إضافة الومضة البصرية
                        link.classList.remove('royal-tap-release');
                        link.classList.add('royal-tap-active');

                    } catch (err) {}
                }
            }, { passive: true });

            // 2. عند التحرك: اكتشاف السكرول (Slop Detection) وإلغاء النقر
            document.addEventListener("touchmove", (e) => {
                if (isScrolling || e.touches.length === 0 || !activeLink) return;
                
                const moveX = Math.abs(e.touches[0].clientX - startX);
                const moveY = Math.abs(e.touches[0].clientY - startY);
                
                // إذا تحرك الإصبع أكثر من 12 بكسل، نعتبره سكرول
                if (moveX > 12 || moveY > 12) {
                    isScrolling = true;
                    activeLink.classList.remove('royal-tap-active');
                    activeLink = null;
                }
            }, { passive: true });

            // 3. عند رفع الإصبع: انتقال فوري (إذا لم يكن سكرول)
            document.addEventListener("touchend", (e) => {
                if (isScrolling || !activeLink) {
                    activeLink = null;
                    return;
                }

                const link = activeLink;
                activeLink = null;

                // إزالة الومضة البصرية بنعومة
                link.classList.remove('royal-tap-active');
                link.classList.add('royal-tap-release');
                setTimeout(() => link.classList.remove('royal-tap-release'), 300);

                // 👑 السحر هنا: انتقال ذكي دون قتل المتصفح
                // لا تتدخل في الروابط الخاصة
                if (
                    link.target === "_blank" ||
                    e.defaultPrevented ||
                    e.metaKey || e.ctrlKey || e.shiftKey
                ) return;

                // فقط الروابط الداخلية
                const isSameOrigin = link.origin === location.origin;

                if (isSameOrigin) {
                    // دعم SPA (إذا الموقع يستخدم pushState)
                    if (window.history && window.history.pushState) {
                        window.location.href = link.href;
                    } else {
                        window.location.assign(link.href);
                    }
                }

            }, { passive: false }); // passive: false ضرورية هنا لكي يعمل preventDefault
        }
    };

    // =========================================================
    // 🛡️ 3. BFCACHE SANITIZER (حارس الرجوع الفوري)
    // =========================================================
    const BFCacheSanitizer = {
        init: function () {
            // إجبار المتصفح على استعادة الصفحة من الذاكرة عند الرجوع
            window.addEventListener('pageshow', function (event) {
                if (event.persisted) {
                    console.log("⏪ ROYAL ENGINE: Page restored instantly from BFCache!");
                    // إذا كان هناك سبلاش معلق، نخفيه فوراً
                    if (window.RoyalJsBridge && typeof window.RoyalJsBridge.hideSplash === 'function') {
                        window.RoyalJsBridge.hideSplash();
                    }
                }
            });
        }
    };

    // =========================================================
    // 🌊 4. SCROLL ENGINE & GESTURE OPTIMIZER
    // =========================================================
    const ScrollEngine = {
        init: function () {
            let lastY = window.scrollY;
            let ticking = false;
            let scrollEndTimer = null;
            let lastTime = 0; 

            function onScroll() {
                if (!document.body.classList.contains("royal-is-scrolling")) {
                    document.body.classList.add("royal-is-scrolling");
                }

                if (scrollEndTimer) clearTimeout(scrollEndTimer);

                scrollEndTimer = setTimeout(() => {
                    document.body.classList.remove("royal-is-scrolling");
                }, 150);

                const now = performance.now();
                if (now - lastTime < 50) return;
                lastTime = now;

                if (!ticking) {
                    requestAnimationFrame(() => {
                        const currentY = window.scrollY;
                        const velocity = currentY - lastY;
                        lastY = currentY;
                        ticking = false;
                    });
                    ticking = true;
                }
            }
            document.addEventListener("scroll", onScroll, { passive: true });
        }
    };

    // =========================================================
    // 🧱 5. RENDER STABILIZER
    // =========================================================
    const RenderStabilizer = {
        init: function () {
            let scheduled = false;
            function stabilize() {
                if (scheduled) return;
                scheduled = true;
                requestAnimationFrame(() => { scheduled = false; });
            }
            const observer = new MutationObserver(stabilize);
            observer.observe(document.body, { childList: true, subtree: true });
        }
    };

    // =========================================================
    // 🚀 IGNITION (كشف العضلة للمايسترو)
    // =========================================================
    function startRoyalInteraction() {
        injectHardwareAcceleration();
        TapEngine.init();
        ScrollEngine.init();
        RenderStabilizer.init();
        BFCacheSanitizer.init(); 
        console.log("⚡ ROYAL INTERACTION ENGINE V4: Instant Click & BFCache Online.");
    }

    // 👑 نكشف الدالة للـ window لكي يستدعيها index.js متى شاء
    window.RoyalInteraction = {
        init: startRoyalInteraction
    };

})();
