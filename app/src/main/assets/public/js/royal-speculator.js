/**
 * =========================================================
 * 🔮 ROYAL SPECULATION ENGINE (V4 - The God Mode Oracle)
 * =========================================================
 * Architecture: Speculation Rules API (Prefetch only), 
 * Fallback to Native Bridge, RAM Protection (Garbage Collection).
 * Philosophy: Render the future before the user clicks.
 */

(function () {
    'use strict';

    // 🧠 ذاكرة المحرك (لحماية الرام من الانفجار)
    const prefetchUrls = new Set();
    const MAX_PREFETCH = 5;  // 5 روابط كحد أقصى للتحميل المسبق

    // التحقق من دعم المتصفح للتقنية النووية (Speculation Rules)
    const supportsSpeculation = HTMLScriptElement.supports && HTMLScriptElement.supports('speculationrules');
    let specScriptElement = null;

    /**
     * 🚀 المحرك المركزي لتحديث قواعد التنبؤ في المتصفح
     */
    function updateSpeculationRules() {
        if (!supportsSpeculation) return;

        if (!specScriptElement) {
            specScriptElement = document.createElement('script');
            specScriptElement.type = 'speculationrules';
            specScriptElement.id = 'royal-speculation-rules';
            document.head.appendChild(specScriptElement);
        }

        const rules = {};
        if (prefetchUrls.size > 0) {
            rules.prefetch =[{ source: "list", urls: Array.from(prefetchUrls) }];
        }

        // حقن القواعد في المتصفح ليقوم بالعمل الثقيل في الخلفية
        specScriptElement.textContent = JSON.stringify(rules);
    }

    /**
     * 🎯 دالة التوجيه الذكية (تقرر هل نستخدم المتصفح أم الجافا)
     */
    function triggerSpeculation(url) {
        if (!url || url.startsWith('javascript:') || url.startsWith('#')) return;
        
        try {
            // حماية صارمة: لا نتنبأ بروابط خارجية أبداً
            if (new URL(url).origin !== window.location.origin) return;
        } catch (e) { return; }

        // منع الصفحات الثقيلة أو الغير مهمة
        if (url.includes('cart') || url.includes('checkout')) return;

        if (supportsSpeculation) {
            // 🟢 استخدام محرك Chromium الحديث
            if (prefetchUrls.has(url)) return;
            if (prefetchUrls.size >= MAX_PREFETCH) return; // نكتفي بـ 5 روابط
            prefetchUrls.add(url);
            updateSpeculationRules();
        } else {
            // 🟡 الخطة ب: استخدام الجسر القديم (Native Java Warmup) للأجهزة القديمة
            if (window.RoyalJsBridge) {
                try { window.RoyalJsBridge.warmup(url); } catch (e) {}
            }
        }
    }

    function findClosestLink(el) {
        while (el && el !== document.body) {
            if (el.tagName === 'A' && el.href) return el;
            el = el.parentElement;
        }
        return null;
    }

    // =========================================================
    // 🟢 LAYER 2: VIEWPORT PREDICTOR (العنف الخفيف - Prefetch)
    // =========================================================
    const ViewportPredictor = {
        init: function () {
            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    const el = entry.target;
                    
                    if (entry.isIntersecting) {
                        // العنصر دخل الشاشة -> قم بتحميله مسبقاً
                        if (el.tagName === 'A' && el.href) triggerSpeculation(el.href);
                    } else {
                        // 🧹 Garbage Collection: العنصر خرج من الشاشة -> احذفه من الذاكرة
                        if (el.tagName === 'A' && el.href && prefetchUrls.has(el.href)) {
                            prefetchUrls.delete(el.href);
                            updateSpeculationRules(); // تحديث المتصفح لتفريغ الرام
                        }
                    }
                });
            }, { rootMargin: "400px" }); // قللنا المساحة لـ 400px ليكون التوقع أدق وأخف

            this.scanDOM = function () {
                document.querySelectorAll('a[href]:not([data-royal-warmed])').forEach(el => {
                    el.setAttribute('data-royal-warmed', 'true');
                    observer.observe(el);
                });
            };

            this.scanDOM();

            // مراقبة التغييرات العميقة لـ React (مع Debounce)
            let scanScheduled = false;

            const mutationObserver = new MutationObserver(() => {
                if (scanScheduled) return;
                scanScheduled = true;

                setTimeout(() => {
                    scanScheduled = false;
                    this.scanDOM();
                }, 200);
            });
            mutationObserver.observe(document.body, { childList: true, subtree: true });
        }
    };

    // =========================================================
    // 🟠 LAYER 3: NAVIGATION PREDICTOR (توقع السكرول السريع)
    // =========================================================
    const NavigationPredictor = {
        init: function () {
            let lastY = window.scrollY;
            let ticking = false;

            document.addEventListener('scroll', () => {
                if (!ticking) {
                    window.requestAnimationFrame(() => {
                        const currentY = window.scrollY;
                        const velocity = currentY - lastY;
                        lastY = currentY;

                        // إذا كان السكرول سريعاً للأسفل، نتوقع الروابط القادمة
                        if (velocity > 30) {
                            this.predictAhead();
                        }
                        ticking = false;
                    });
                    ticking = true;
                }
            }, { passive: true });
        },

        predictAhead: function () {
            const links = document.querySelectorAll('a[href]:not([data-royal-warmed])');
            let warmedCount = 0;
           
            for (let i = 0; i < links.length && warmedCount < 2; i++) { // نكتفي برابطين فقط أثناء السكرول
                if (links[i].href) {
                    triggerSpeculation(links[i].href);
                    links[i].setAttribute('data-royal-warmed', 'true');
                    warmedCount++;
                }
            }
        }
    };

    // =========================================================
    // 🚀 IGNITION (كشف العضلة للمايسترو)
    // =========================================================
    function startEngine() {
        ViewportPredictor.init();
        NavigationPredictor.init();
        
        console.log(`🔮 ROYAL SPECULATION ENGINE V4: Online. (Native Support: ${supportsSpeculation})`);
    }

    // 👑 نكشف الدالة للـ window لكي يستدعيها index.js في وقت الفراغ
    window.RoyalSpeculator = {
        init: startEngine
    };

})();
