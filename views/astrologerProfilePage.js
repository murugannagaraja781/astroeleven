/**
 * Astrologer Profile Web Page Renderer
 * Renders modern, responsive HTML with OpenGraph tags and App Deep Links
 */
function renderAstrologerProfileHtml(astro, serverUrl) {
  const name = astro.name || 'Astrologer';
  const skills = (astro.skills && astro.skills.length > 0) ? astro.skills.join(', ') : 'Vedic Astrology, Kundli Reading';
  const skillsArray = (astro.skills && astro.skills.length > 0) ? astro.skills : ['Vedic Astrology', 'Kundli', 'Horoscope Reading'];
  const exp = astro.experience ? `${astro.experience}+ Years Exp` : '5+ Years Exp';
  const chatPrice = astro.chatPrice || astro.price || 15;
  const callPrice = astro.callPrice || astro.price || 15;
  const videoPrice = astro.videoPrice || 20;
  const image = astro.image || `${serverUrl}/images/astrologer_hero.png`;
  const languages = (astro.languages && astro.languages.length > 0) ? astro.languages.join(', ') : 'Tamil, English';
  
  const isOnline = !!(astro.isOnline || astro.isChatOnline || astro.isAudioOnline || astro.isVideoOnline);
  const isBusy = !!astro.isBusy;
  const statusText = isOnline ? 'Online Now' : (isBusy ? 'Busy in Session' : 'Offline');
  const statusClass = isOnline ? 'status-online' : (isBusy ? 'status-busy' : 'status-offline');
  const statusDotColor = isOnline ? '#22C55E' : (isBusy ? '#EF4444' : '#9CA3AF');

  const profileUrl = `${serverUrl}/astrologer/${astro.userId}`;
  const deepLink = `astroeleven://astrologer/${astro.userId}`;
  const intentUrl = `intent://astrologer/${astro.userId}#Intent;scheme=astroeleven;package=com.astroeleven.app;end`;
  const playStoreUrl = `https://play.google.com/store/search?q=astroeleven&c=apps`;
  const apkDownloadUrl = `${serverUrl}/downloads/astroeleven-latest.apk`;
  const webConsultUrl = `${serverUrl}/?astro=${astro.userId}`;

  return `<!DOCTYPE html>
<html lang="ta,en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>${name} - Certified Astrologer | Astro Eleven</title>

  <!-- OpenGraph / WhatsApp Meta Tags -->
  <meta property="og:type" content="profile">
  <meta property="og:title" content="Consult with ${name} on Astro Eleven">
  <meta property="og:description" content="Talk or Chat with ${name} (${skills}) on Astro Eleven. Rate: ₹${chatPrice}/min. Accurate predictions & instant remedies.">
  <meta property="og:image" content="${image}">
  <meta property="og:image:width" content="600">
  <meta property="og:image:height" content="600">
  <meta property="og:url" content="${profileUrl}">
  <meta property="og:site_name" content="Astro Eleven">

  <!-- Twitter Meta Tags -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="Consult with ${name} on Astro Eleven">
  <meta name="twitter:description" content="Talk or Chat with ${name} (${skills}) - ₹${chatPrice}/min. Certified Vedic Astrologer on Astro Eleven.">
  <meta name="twitter:image" content="${image}">

  <link rel="icon" type="image/png" href="/favicon.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Poppins:wght@300;400;500;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

  <style>
    :root {
      --primary: #E87A1E;
      --primary-gradient: linear-gradient(135deg, #E87A1E 0%, #D84315 100%);
      --accent-gold: #F59E0B;
      --bg-dark: #0F172A;
      --card-bg: rgba(30, 41, 59, 0.85);
      --card-border: rgba(232, 122, 30, 0.25);
      --text-white: #FFFFFF;
      --text-muted: #94A3B8;
      --radius: 20px;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: 'Poppins', sans-serif;
      background: #0B0F19;
      color: var(--text-white);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      position: relative;
      overflow-x: hidden;
    }

    /* Ambient Cosmic Glow */
    body::before {
      content: '';
      position: fixed;
      top: -20%;
      left: 50%;
      transform: translateX(-50%);
      width: 600px;
      height: 600px;
      background: radial-gradient(circle, rgba(232, 122, 30, 0.18) 0%, rgba(15, 23, 42, 0) 70%);
      pointer-events: none;
      z-index: 0;
    }

    /* Navigation Bar */
    header {
      position: sticky;
      top: 0;
      z-index: 50;
      background: rgba(15, 23, 42, 0.9);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      padding: 12px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      color: #fff;
    }

    .brand img {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      border: 1px solid rgba(232, 122, 30, 0.4);
    }

    .brand-title {
      font-family: 'Outfit', sans-serif;
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #FFF 0%, #FFB74D 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .header-btn {
      background: var(--primary-gradient);
      color: #fff;
      padding: 8px 16px;
      border-radius: 999px;
      font-size: 0.85rem;
      font-weight: 600;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 6px;
      box-shadow: 0 4px 14px rgba(232, 122, 30, 0.35);
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .header-btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(232, 122, 30, 0.5);
    }

    /* Main Container */
    main {
      flex: 1;
      max-width: 600px;
      width: 100%;
      margin: 0 auto;
      padding: 20px 16px 40px;
      z-index: 1;
    }

    /* Astrologer Card */
    .profile-card {
      background: var(--card-bg);
      backdrop-filter: blur(16px);
      border: 1px solid var(--card-border);
      border-radius: var(--radius);
      padding: 28px 20px;
      text-align: center;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
      position: relative;
      margin-top: 10px;
    }

    /* Avatar with Status */
    .avatar-wrapper {
      position: relative;
      width: 130px;
      height: 130px;
      margin: 0 auto 16px;
    }

    .avatar-img {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid #E87A1E;
      box-shadow: 0 8px 24px rgba(232, 122, 30, 0.3);
    }

    .status-badge {
      position: absolute;
      bottom: 6px;
      right: 6px;
      background: #0F172A;
      border: 2px solid ${statusDotColor};
      color: #fff;
      padding: 3px 10px;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 5px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.5);
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: ${statusDotColor};
      display: inline-block;
      ${isOnline ? 'animation: pulse 1.8s infinite;' : ''}
    }

    @keyframes pulse {
      0% { transform: scale(0.95); opacity: 0.8; box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
      70% { transform: scale(1.1); opacity: 1; box-shadow: 0 0 0 8px rgba(34, 197, 94, 0); }
      100% { transform: scale(0.95); opacity: 0.8; box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
    }

    /* Profile Details */
    .astro-name {
      font-family: 'Outfit', sans-serif;
      font-size: 1.7rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #FFFFFF;
      margin-bottom: 4px;
    }

    .verified-icon {
      color: #38BDF8;
      font-size: 1.15rem;
    }

    .astro-meta {
      font-size: 0.88rem;
      color: var(--text-muted);
      margin-bottom: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
    }

    .astro-meta span {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .rating-badge {
      color: #F59E0B;
      font-weight: 600;
    }

    /* Skills Chips */
    .skills-container {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
      margin: 16px 0 24px;
    }

    .skill-tag {
      background: rgba(232, 122, 30, 0.12);
      border: 1px solid rgba(232, 122, 30, 0.28);
      color: #FDBA74;
      font-size: 0.78rem;
      font-weight: 500;
      padding: 5px 12px;
      border-radius: 999px;
    }

    /* Price Consultation Grid */
    .price-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
      margin-bottom: 24px;
    }

    .price-box {
      background: rgba(15, 23, 42, 0.65);
      border: 1px solid rgba(255, 255, 255, 0.07);
      border-radius: 14px;
      padding: 12px 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      transition: border-color 0.2s;
    }

    .price-box:hover {
      border-color: rgba(232, 122, 30, 0.4);
    }

    .price-box i {
      font-size: 1.25rem;
      color: #E87A1E;
      margin-bottom: 2px;
    }

    .price-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .price-val {
      font-family: 'Outfit', sans-serif;
      font-size: 1.15rem;
      font-weight: 700;
      color: #FFF;
    }

    .price-val span {
      font-size: 0.75rem;
      font-weight: 400;
      color: var(--text-muted);
    }

    /* Primary Action Buttons */
    .action-group {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 10px;
    }

    .btn-primary-app {
      background: var(--primary-gradient);
      color: #FFFFFF;
      border: none;
      padding: 15px 24px;
      border-radius: 14px;
      font-size: 1.05rem;
      font-weight: 700;
      text-decoration: none;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      box-shadow: 0 8px 24px rgba(232, 122, 30, 0.45);
      transition: transform 0.2s, box-shadow 0.2s;
      cursor: pointer;
    }

    .btn-primary-app:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 30px rgba(232, 122, 30, 0.6);
    }

    .btn-secondary {
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.12);
      color: #FFFFFF;
      padding: 13px 20px;
      border-radius: 14px;
      font-size: 0.95rem;
      font-weight: 600;
      text-decoration: none;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: background 0.2s, border-color 0.2s;
    }

    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.25);
    }

    .btn-web {
      background: transparent;
      color: #FDBA74;
      border: 1px dashed rgba(232, 122, 30, 0.4);
      padding: 11px 16px;
      border-radius: 14px;
      font-size: 0.88rem;
      font-weight: 500;
      text-decoration: none;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }

    .btn-web:hover {
      background: rgba(232, 122, 30, 0.08);
    }

    /* Trust & Guarantee Section */
    .trust-row {
      display: flex;
      justify-content: space-around;
      margin-top: 24px;
      padding-top: 20px;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      font-size: 0.78rem;
      color: var(--text-muted);
    }

    .trust-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
    }

    .trust-item i {
      font-size: 1.1rem;
      color: #E87A1E;
    }

    /* Footer */
    footer {
      text-align: center;
      padding: 20px;
      font-size: 0.8rem;
      color: var(--text-muted);
      border-top: 1px solid rgba(255, 255, 255, 0.06);
    }

    footer a {
      color: #FDBA74;
      text-decoration: none;
    }
  </style>
</head>
<body>

  <!-- Header -->
  <header>
    <a href="/" class="brand">
      <img src="/favicon.png" alt="Astro Eleven Logo">
      <span class="brand-title">Astro Eleven</span>
    </a>
    <a href="${playStoreUrl}" class="header-btn" id="btnInstallHeader">
      <i class="fab fa-google-play"></i> Get App
    </a>
  </header>

  <!-- Main Profile -->
  <main>
    <div class="profile-card">
      <!-- Avatar -->
      <div class="avatar-wrapper">
        <img src="${image}" alt="${name}" class="avatar-img" onerror="this.src='/images/astrologer_hero.png'">
        <div class="status-badge">
          <span class="status-dot"></span>
          <span>${statusText}</span>
        </div>
      </div>

      <!-- Name & Credentials -->
      <h1 class="astro-name">
        ${name}
        <i class="fas fa-certificate verified-icon" title="Certified Astrologer"></i>
      </h1>

      <div class="astro-meta">
        <span class="rating-badge"><i class="fas fa-star"></i> 4.9 (1.2k+)</span>
        <span>•</span>
        <span><i class="fas fa-user-clock"></i> ${exp}</span>
        <span>•</span>
        <span><i class="fas fa-language"></i> ${languages}</span>
      </div>

      <!-- Skills -->
      <div class="skills-container">
        ${skillsArray.map(s => `<span class="skill-tag">${s.trim()}</span>`).join('')}
      </div>

      <!-- Pricing -->
      <div class="price-grid">
        <div class="price-box">
          <i class="fas fa-comments"></i>
          <span class="price-label">Chat</span>
          <span class="price-val">₹${chatPrice}<span>/min</span></span>
        </div>
        <div class="price-box">
          <i class="fas fa-phone-alt"></i>
          <span class="price-label">Audio</span>
          <span class="price-val">₹${callPrice}<span>/min</span></span>
        </div>
        <div class="price-box">
          <i class="fas fa-video"></i>
          <span class="price-label">Video</span>
          <span class="price-val">₹${videoPrice}<span>/min</span></span>
        </div>
      </div>

      <!-- Call to Actions -->
      <div class="action-group">
        <!-- Direct App Deep Link Button -->
        <a href="${deepLink}" class="btn-primary-app" id="btnOpenApp">
          <i class="fas fa-bolt"></i> Consult Now in App
        </a>

        <!-- Google Play Store -->
        <a href="${playStoreUrl}" class="btn-secondary" id="btnPlayStore">
          <i class="fab fa-google-play"></i> Install from Google Play
        </a>

        <!-- Direct APK Download -->
        <a href="${apkDownloadUrl}" class="btn-secondary" id="btnDownloadApk">
          <i class="fas fa-download"></i> Direct APK Download (Android)
        </a>

        <!-- Web Browser Consultation -->
        <a href="${webConsultUrl}" class="btn-web">
          <i class="fas fa-globe"></i> Or Continue on Web Browser
        </a>
      </div>

      <!-- Trust Badges -->
      <div class="trust-row">
        <div class="trust-item">
          <i class="fas fa-shield-alt"></i>
          <span>100% Verified</span>
        </div>
        <div class="trust-item">
          <i class="fas fa-lock"></i>
          <span>Private & Secure</span>
        </div>
        <div class="trust-item">
          <i class="fas fa-bolt"></i>
          <span>Instant Connect</span>
        </div>
      </div>

    </div>
  </main>

  <!-- Footer -->
  <footer>
    <p>© 2026 Astro Eleven. India's Trusted Astrology Platform.</p>
    <p style="margin-top: 4px;">
      <a href="/privacy-policy">Privacy Policy</a> • 
      <a href="/terms-condition">Terms of Service</a>
    </p>
  </footer>

  <script>
    // Seamless Deep Link Attempt on Mobile
    (function() {
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      const isAndroid = /Android/i.test(navigator.userAgent);
      const appDeepLink = "${deepLink}";
      const intentUrl = "${intentUrl}";

      // Handle "Consult Now in App" button click
      const btnOpenApp = document.getElementById('btnOpenApp');
      if (btnOpenApp) {
        btnOpenApp.addEventListener('click', function(e) {
          if (isAndroid) {
            // Prefer Android intent URI for highest compatibility
            window.location.href = intentUrl;
            setTimeout(() => {
              window.location.href = appDeepLink;
            }, 600);
          } else {
            window.location.href = appDeepLink;
          }
        });
      }

      // Auto-attempt opening the app if opened on a mobile device
      if (isMobile) {
        try {
          if (isAndroid) {
            window.location.href = intentUrl;
          } else {
            window.location.href = appDeepLink;
          }
        } catch(err) {}
      }
    })();
  </script>
</body>
</html>`;
}

module.exports = { renderAstrologerProfileHtml };
