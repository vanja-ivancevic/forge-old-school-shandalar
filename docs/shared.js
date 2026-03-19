// ~*~ Old Border Shandalar - Shared Components ~*~
// Replaces duplicated sidebar, mobile nav, and ribbon across all pages.
// Uses document.write() for synchronous injection (no FOUC) — just like the 90s!
// NOTE: All HTML here is static hardcoded strings — no user input, no XSS vector.

(function() {

  // === ACTIVE PAGE DETECTION ===
  var page = location.pathname.split('/').pop() || 'index.html';
  if (page === '' || page === 'docs') page = 'index.html';

  function activeClass(href) {
    return page === href ? ' active' : '';
  }

  // === FAVICON ===
  if (!document.querySelector('link[rel="icon"]')) {
    var link = document.createElement('link');
    link.rel = 'icon';
    link.type = 'image/svg+xml';
    link.href = 'favicon.svg';
    document.head.appendChild(link);
  }

  // === CORNER RIBBON ===
  document.write('<div class="corner-ribbon"><span>100% FREE</span>OPEN SOURCE</div>');

  // === MOBILE NAV ===
  document.write(
    '<div class="mobile-nav">' +
    '<a href="index.html">Home</a>' +
    '<a href="features.html">Features</a>' +
    '<a href="howtoplay.html">How to Play</a>' +
    '<a href="guide.html">Guide</a>' +
    '<a href="changelog.html">Changelog</a>' +
    '<a href="community.html">Community</a>' +
    '<a href="decks.html">Decks</a>' +
    '<a href="modern/' + page + '" class="mobile-modern-link">&#10022; Modern</a>' +
    '</div>'
  );

  // === TABLE + SIDEBAR ===
  document.write(
    '<table class="layout-table" cellpadding="0" cellspacing="0" border="0"><tr>' +
    '<td class="nav-sidebar"><div class="nav-inner">' +

    '<center>' +
    '<img src="gifs/skull3.gif" alt="*" width="30" height="33"><br>' +
    '<div class="nav-title">~*~ Old Border Shandalar ~*~</div>' +
    '</center>' +

    '<a href="index.html" class="nav-link' + activeClass('index.html') + '">&#9733; Home</a>' +
    '<a href="features.html" class="nav-link' + activeClass('features.html') + '">&#9876; Features</a>' +
    '<a href="howtoplay.html" class="nav-link' + activeClass('howtoplay.html') + '">&#9654; How to Play</a>' +
    '<a href="guide.html" class="nav-link' + activeClass('guide.html') + '">&#128214; Guide</a>' +
    '<a href="changelog.html" class="nav-link' + activeClass('changelog.html') + '">&#128240; Changelog</a>' +
    '<a href="community.html" class="nav-link' + activeClass('community.html') + '">&#128172; Community</a>' +
    '<a href="decks.html" class="nav-link' + activeClass('decks.html') + '">&#9824; Decks</a>' +

    '<hr class="nav-separator">' +

    '<a href="modern/' + page + '" class="nav-link nav-modern-link">&#10022; Modern Site</a>' +

    '<hr class="nav-separator">' +

    '<center>' +
    '<font size="2" color="#666688"><br>' +
    '<img src="gifs/wizard3.gif" alt="*~magic~*" width="80" height="57"><br><br>' +
    '<font color="#8866AA">Visitors:</font><br>' +
    '<span id="visitor-counter">' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '<span class="counter-digit" style="font-size:11px;padding:1px 3px">-</span>' +
    '</span><br><br>' +
    '<img src="gifs/netscape.gif" alt="Netscape Now!" width="88" height="31" title="Netscape Now!"><br>' +
    '<img src="gifs/besteyes.gif" alt="Best viewed with eyes" width="88" height="31" title="Best Viewed With Eyes"><br>' +
    '<img src="gifs/java.gif" alt="Java" width="88" height="31" title="Get Java">' +
    '<br><br>' +
    '<font size="2" color="#444466">Best viewed at<br>800x600</font>' +
    '</font></center>' +

    '</div></td>'
  );

  // === LIVE VISITOR COUNTER (CounterAPI.dev) ===
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('visitor-counter');
    if (!el) return;

    var base = 'https://api.counterapi.dev/v1/old-border-shandalar/visits';
    var counted = sessionStorage.getItem('obs-counted');
    var url = counted ? base + '/' : base + '/up';

    fetch(url).then(function(r) { return r.json(); }).then(function(data) {
      if (!counted) sessionStorage.setItem('obs-counted', '1');
      var digits = String(data.count).padStart(6, '0').split('');
      var spans = el.querySelectorAll('.counter-digit');
      for (var i = 0; i < spans.length; i++) {
        spans[i].textContent = digits[i] || '0';
      }
    }).catch(function() {
      // API down — leave dashes as placeholder
    });
  });

})();
