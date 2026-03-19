// ~*~ Genie - Your Helpful Office Assistant ~*~
// Uses the REAL Microsoft Agent from clippyjs (actual sprite sheets)
import { initAgent } from "https://cdn.jsdelivr.net/npm/clippyjs/dist/index.mjs";
import { Genie } from "https://cdn.jsdelivr.net/npm/clippyjs/dist/agents/index.mjs";

(async function() {
  // Don't show on modern pages
  if (document.body.getAttribute('data-theme') === 'modern') return;

  // Check if user already dismissed (persists across sessions)
  if (localStorage.getItem('clippy-dismissed')) return;

  // Determine the modern page URL
  var currentPage = window.location.pathname.split('/').pop() || 'index.html';
  var modernUrl = 'modern/' + currentPage;

  // Load the REAL Genie agent (no audio)
  var _origPlay = HTMLAudioElement.prototype.play;
  HTMLAudioElement.prototype.play = function() { return Promise.resolve(); };

  var agent;
  try {
    agent = await initAgent(Genie);
  } catch (e) {
    console.warn('Genie failed to load:', e);
    return;
  }

  // Position in the bottom-right corner
  agent.show();
  agent.moveTo(window.innerWidth - 160, window.innerHeight - 140);

  // Build the custom speech bubble with buttons (safe DOM construction)
  var container = document.createElement('div');
  container.id = 'clippy-speech';

  var dismissBtn = document.createElement('button');
  dismissBtn.id = 'clippy-dismiss';
  dismissBtn.title = 'Close';
  dismissBtn.textContent = '\u00d7';
  container.appendChild(dismissBtn);

  var text = document.createElement('div');
  text.appendChild(document.createTextNode('It looks like you\'re viewing a website from '));
  var bold = document.createElement('b');
  bold.textContent = '1997';
  text.appendChild(bold);
  text.appendChild(document.createTextNode('!'));
  text.appendChild(document.createElement('br'));
  text.appendChild(document.createElement('br'));
  text.appendChild(document.createTextNode('Would you prefer the modern, clean, '));
  var italic = document.createElement('i');
  italic.textContent = 'lame';
  text.appendChild(italic);
  text.appendChild(document.createTextNode(' version instead?'));
  container.appendChild(text);

  var buttons = document.createElement('div');
  buttons.id = 'clippy-buttons';

  var yesBtn = document.createElement('button');
  yesBtn.textContent = 'Yes, I hate fun';
  yesBtn.addEventListener('click', function() {
    window.location.href = modernUrl;
  });
  buttons.appendChild(yesBtn);

  var noBtn = document.createElement('button');
  noBtn.textContent = 'No, this is perfect';
  noBtn.addEventListener('click', function() {
    dismissClippy();
  });
  buttons.appendChild(noBtn);

  container.appendChild(buttons);
  document.body.appendChild(container);

  // Position the speech bubble near clippy
  positionBubble();
  window.addEventListener('resize', positionBubble);

  function positionBubble() {
    container.style.position = 'fixed';
    container.style.bottom = '240px';
    container.style.right = '20px';
    container.style.zIndex = '10001';
  }

  function dismissClippy() {
    container.style.display = 'none';
    localStorage.setItem('clippy-dismissed', '1');
    agent.play('GoodBye');
    setTimeout(function() { agent.hide(); }, 2000);
  }

  dismissBtn.addEventListener('click', function(e) {
    e.stopPropagation();
    dismissClippy();
  });

  // Let Genie do some idle animations
  setTimeout(function() {
    agent.animate();
  }, 3000);
})();
