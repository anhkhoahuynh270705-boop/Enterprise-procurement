document.addEventListener('DOMContentLoaded', () => {
  const video = document.querySelector('.enterprise-background');
  if (!video) return;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const syncPreference = () => {
    if (reducedMotion.matches) {
      video.pause();
      video.removeAttribute('autoplay');
    } else {
      video.muted = true;
      video.play().catch(() => {});
    }
  };
  reducedMotion.addEventListener('change', syncPreference);
  syncPreference();
});