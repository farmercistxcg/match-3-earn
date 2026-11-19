/* ============================================================================
   Match-3 Earn — Supabase config (PLACEHOLDER in the repo)
   ----------------------------------------------------------------------------
   The real Project URL and anon key are injected at build time from the
   GitHub repo secrets SUPABASE_URL / SUPABASE_ANON_KEY (see README.md and
   .github/workflows/build-apk.yml). The file is then inlined into
   index.html by tools/inline_config.py, because the WebView loads the
   game as a string and a separate <script src> would never resolve.
   The anon key is safe to ship inside the app (it is the PUBLIC key);
   NEVER put the service_role key in this file or in the app.
   ========================================================================== */
window.SUPABASE_CONFIG = {
  url: "https://YOUR-PROJECT.supabase.co",
  anonKey: "YOUR-ANON-KEY"
};
