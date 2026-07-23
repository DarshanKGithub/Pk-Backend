package com.pkcorporate.config;

// ─── DELETED ──────────────────────────────────────────────────────────────────
// This file has been intentionally emptied.
//
// Step 5 of the low-compute optimisation plan: the custom Servlet filter that
// lived here ran at Ordered.HIGHEST_PRECEDENCE and set Access-Control-* headers
// before Spring Security could, causing every request to be CORS-processed twice.
//
// Spring Security's CorsConfigurationSource (configured in SecurityConfig via
// cors -> cors.configurationSource(corsConfigurationSource())) now handles all
// CORS. It supports the same allowed-origin patterns, wildcard Vercel subdomains,
// and Capacitor localhost that the old filter did.
//
// If you need to re-introduce a raw Servlet CORS filter for some reason, remove
// Spring Security's .cors(...) call first — never run both at once.
// ──────────────────────────────────────────────────────────────────────────────
