// ~*~ Old Border Shandalar - Deck Gallery ~*~
// Shared module for deck submission, browsing, voting, and Scryfall integration.
// Uses Firebase (Firestore + Anonymous Auth) and Scryfall API.

import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.0/firebase-app.js';
import { getFirestore, collection, doc, getDoc, getDocs, addDoc, setDoc, deleteDoc, updateDoc,
         query, where, orderBy, limit, startAfter, increment, serverTimestamp, Timestamp
} from 'https://www.gstatic.com/firebasejs/10.14.0/firebase-firestore.js';
import { getAuth, signInAnonymously, onAuthStateChanged
} from 'https://www.gstatic.com/firebasejs/10.14.0/firebase-auth.js';

// ============================================================
// FIREBASE CONFIG — Replace with your project's config
// ============================================================
const firebaseConfig = {
  apiKey: "AIzaSyAoXwBppMdI3NyGHIyp5UyaM0ncIMlDFlk",
  authDomain: "decklists-3a783.firebaseapp.com",
  projectId: "decklists-3a783",
  storageBucket: "decklists-3a783.firebasestorage.app",
  messagingSenderId: "1067241798681",
  appId: "1:1067241798681:web:ad2eff2cfc09b28b6bbe88"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// ============================================================
// AUTH
// ============================================================
let currentUser = null;

/** Sign in anonymously. Returns the Firebase user. */
export async function ensureAuth() {
  if (currentUser) return currentUser;
  await signInAnonymously(auth);
  return new Promise(resolve => {
    const unsub = onAuthStateChanged(auth, user => {
      if (user) {
        currentUser = user;
        unsub();
        resolve(user);
      }
    });
  });
}

/** Get current user (may be null if not yet authed). */
export function getUser() {
  return currentUser;
}

// Auto-listen for auth state
onAuthStateChanged(auth, user => { currentUser = user; });

// ============================================================
// DECK PARSERS
// ============================================================

/**
 * Auto-detect format and parse deck text.
 * Returns { title, cards: [{ qty, name, set?, artIndex? }], errors: [] }
 */
export function parseDeck(text) {
  text = text.trim();
  if (!text) return { title: '', cards: [], errors: ['Empty deck list'] };

  // Detect Forge clipboard format: starts with "Deck:" or has "Main:" section
  if (/^Deck:/m.test(text) || /^Main:/m.test(text)) {
    return parseForgeClipboard(text);
  }
  // Detect .dck format: lines contain pipe separators like "1 Card Name|SET|1"
  if (/^\d+\s+.+\|.+\|/m.test(text)) {
    return parseDck(text);
  }
  // Fall back to simple list: "4 Card Name"
  return parseSimpleList(text);
}

/** Parse Forge clipboard: "Deck: Name\n\nMain:\n4 Card Name\n...\nSideboard:\n..." */
function parseForgeClipboard(text) {
  var title = '';
  var cards = [];
  var sideboard = [];
  var errors = [];
  var currentSection = null; // 'main', 'sideboard', or null

  var lines = text.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim();
    if (!line) continue;

    if (line.startsWith('Deck:')) {
      title = line.substring(5).trim();
      continue;
    }
    if (/^(Main|Mainboard|Card):$/i.test(line)) {
      currentSection = 'main';
      continue;
    }
    if (/^(Sideboard|Side|SB):$/i.test(line)) {
      currentSection = 'sideboard';
      continue;
    }
    if (/^(Avatar|Commander|Schemes|Conspiracy|Planes|Attractions|Contraptions):$/i.test(line)) {
      currentSection = null;
      continue;
    }

    // If we haven't seen "Main:" yet, treat lines after "Deck:" as main
    if (!currentSection && title && cards.length === 0 && /^\d+\s+/.test(line)) {
      currentSection = 'main';
    }

    if (currentSection === 'main' || currentSection === 'sideboard') {
      var parsed = parseCardLine(line);
      if (parsed) {
        if (currentSection === 'sideboard') sideboard.push(parsed);
        else cards.push(parsed);
      } else {
        errors.push('Could not parse: ' + line);
      }
    }
  }

  return { title: title, cards: cards, sideboard: sideboard, errors: errors };
}

/** Parse .dck format: "1 Card Name|SET|ArtIndex" */
function parseDck(text) {
  var cards = [];
  var sideboard = [];
  var errors = [];
  var currentSection = 'main'; // default to main

  var lines = text.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim();
    if (!line) continue;
    if (line.startsWith('[')) {
      if (/^\[Main\]$/i.test(line) || /^\[metadata\]$/i.test(line)) currentSection = 'main';
      else if (/^\[Sideboard\]$/i.test(line)) currentSection = 'sideboard';
      else currentSection = null;
      continue;
    }
    if (currentSection === null) continue;
    if (line.startsWith('Name=') || line.startsWith('Comment=') || line.startsWith('Tags=') || line.startsWith('DraftNotes=') || line.startsWith('AiHints=')) continue;

    var match = line.match(/^(\d+)\s+(.+?)(?:\|([A-Z0-9]+)\|(\d+))?$/);
    if (match) {
      var entry = {
        qty: parseInt(match[1], 10),
        name: match[2].trim(),
        set: match[3] || null,
        artIndex: match[4] ? parseInt(match[4], 10) : null
      };
      if (currentSection === 'sideboard') sideboard.push(entry);
      else cards.push(entry);
    } else {
      errors.push('Could not parse: ' + line);
    }
  }

  return { title: '', cards: cards, sideboard: sideboard, errors: errors };
}

/** Parse simple list: "4 Card Name" or "4x Card Name" */
function parseSimpleList(text) {
  var cards = [];
  var sideboard = [];
  var errors = [];
  var inSideboard = false;

  var lines = text.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim();
    if (!line) continue;

    if (/^(Sideboard|Side|SB):?$/i.test(line)) {
      inSideboard = true;
      continue;
    }

    var parsed = parseCardLine(line);
    if (parsed) {
      if (inSideboard) sideboard.push(parsed);
      else cards.push(parsed);
    } else {
      errors.push('Could not parse: ' + line);
    }
  }

  return { title: '', cards: cards, sideboard: sideboard, errors: errors };
}

/** Parse a single card line like "4 Lightning Bolt" or "4x Lightning Bolt" */
function parseCardLine(line) {
  var match = line.match(/^(\d+)x?\s+(.+)$/);
  if (!match) return null;
  return {
    qty: parseInt(match[1], 10),
    name: match[2].trim()
  };
}

// ============================================================
// SCRYFALL INTEGRATION
// ============================================================

const SCRYFALL_API = 'https://api.scryfall.com';

/**
 * Validate cards against Scryfall and enrich with image URLs + color identity.
 * Fetches the OLDEST printing of each card (old-border preferred).
 * @param {Array} cards - Main deck [{ qty, name }]
 * @param {Array} [sideboard] - Sideboard [{ qty, name }]
 * @returns { validCards, validSideboard, invalidNames, colorIdentity, colorCode }
 */
export async function validateAndEnrichCards(cards, sideboard) {
  sideboard = sideboard || [];
  var allCards = cards.concat(sideboard);

  // Deduplicate by name for the API call
  var uniqueNames = [];
  var seen = {};
  for (var i = 0; i < allCards.length; i++) {
    var lower = allCards[i].name.toLowerCase();
    if (!seen[lower]) {
      seen[lower] = true;
      uniqueNames.push(allCards[i].name);
    }
  }

  // Step 1: Validate all cards via /cards/collection (fast batch lookup)
  // For split cards ("Fire // Ice"), Scryfall collection API only accepts the first half
  var splitNameMap = {}; // maps normalized first-half name -> original full name
  var normalizedNames = uniqueNames.map(function(name) {
    if (name.indexOf(' // ') !== -1) {
      var firstHalf = name.split(' // ')[0].trim();
      splitNameMap[firstHalf.toLowerCase()] = name.toLowerCase();
      return firstHalf;
    }
    return name;
  });

  var batches = [];
  for (var j = 0; j < normalizedNames.length; j += 75) {
    batches.push(normalizedNames.slice(j, j + 75));
  }

  var cardDataMap = {}; // lowercase name -> scryfall data
  var invalidNames = [];

  for (var b = 0; b < batches.length; b++) {
    var identifiers = batches[b].map(function(name) { return { name: name }; });
    if (b > 0) await sleep(100);

    var resp = await fetch(SCRYFALL_API + '/cards/collection', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifiers: identifiers })
    });

    if (!resp.ok) throw new Error('Scryfall API error: ' + resp.status);
    var data = await resp.json();

    if (data.data) {
      for (var k = 0; k < data.data.length; k++) {
        var card = data.data[k];
        cardDataMap[card.name.toLowerCase()] = card;
        // Also map the original full split name (e.g. "fire // ice") to this card
        var origName = splitNameMap[card.name.split(' // ')[0].toLowerCase()];
        if (origName) cardDataMap[origName] = card;
      }
    }
    if (data.not_found) {
      for (var n = 0; n < data.not_found.length; n++) {
        invalidNames.push(data.not_found[n].name || data.not_found[n].id);
      }
    }
  }

  // Step 2: For each validated card, fetch oldest printing image
  // Uses /cards/search with order:released dir:asc to get the earliest printing
  var oldestImgMap = {}; // lowercase name -> { small, normal }
  var validatedNames = Object.keys(cardDataMap);

  for (var oi = 0; oi < validatedNames.length; oi++) {
    var canonicalName = cardDataMap[validatedNames[oi]].name;
    await sleep(80); // Respect rate limit (~10 req/s)

    try {
      var searchResp = await fetch(
        SCRYFALL_API + '/cards/search?q=' +
        encodeURIComponent('!"' + canonicalName + '"') +
        '&order=released&dir=asc&unique=prints'
      );

      if (searchResp.ok) {
        var searchData = await searchResp.json();
        if (searchData.data && searchData.data.length > 0) {
          var oldest = searchData.data[0];
          var imgs = oldest.image_uris ||
            (oldest.card_faces && oldest.card_faces[0] && oldest.card_faces[0].image_uris) || {};
          oldestImgMap[validatedNames[oi]] = {
            small: imgs.small || '',
            normal: imgs.normal || ''
          };
        }
      }
    } catch(e) {
      // Fall back to default printing images
    }
  }

  // Step 3: Enrich card arrays with Scryfall data + oldest printing images
  function enrichArray(arr) {
    var result = [];
    for (var c = 0; c < arr.length; c++) {
      var cardEntry = arr[c];
      var scryfallData = cardDataMap[cardEntry.name.toLowerCase()];
      if (!scryfallData) continue;

      var oldImgs = oldestImgMap[cardEntry.name.toLowerCase()];
      var imgSmall = '';
      var imgNormal = '';

      if (oldImgs && oldImgs.small) {
        imgSmall = oldImgs.small;
        imgNormal = oldImgs.normal;
      } else if (scryfallData.image_uris) {
        imgSmall = scryfallData.image_uris.small || '';
        imgNormal = scryfallData.image_uris.normal || '';
      } else if (scryfallData.card_faces && scryfallData.card_faces[0] && scryfallData.card_faces[0].image_uris) {
        imgSmall = scryfallData.card_faces[0].image_uris.small || '';
        imgNormal = scryfallData.card_faces[0].image_uris.normal || '';
      }

      result.push({
        qty: cardEntry.qty,
        name: scryfallData.name,
        set: cardEntry.set || null,
        scryfallImg: imgSmall,
        scryfallImgNormal: imgNormal,
        manaCost: scryfallData.mana_cost || '',
        typeLine: scryfallData.type_line || ''
      });
    }
    return result;
  }

  var validCards = enrichArray(cards);
  var validSideboard = enrichArray(sideboard);

  // Collect color identity from main deck only
  var colorIdentitySet = {};
  for (var c = 0; c < validCards.length; c++) {
    var sd = cardDataMap[validCards[c].name.toLowerCase()];
    if (sd && sd.color_identity) {
      for (var ci = 0; ci < sd.color_identity.length; ci++) {
        colorIdentitySet[sd.color_identity[ci]] = true;
      }
    }
  }

  var wubrgOrder = ['W', 'U', 'B', 'R', 'G'];
  var colorIdentity = wubrgOrder.filter(function(c) { return colorIdentitySet[c]; });
  var colorCode = colorIdentity.join('');

  return {
    validCards: validCards,
    validSideboard: validSideboard,
    invalidNames: invalidNames,
    colorIdentity: colorIdentity,
    colorCode: colorCode
  };
}

function sleep(ms) {
  return new Promise(function(resolve) { setTimeout(resolve, ms); });
}

// ============================================================
// DECK SUBMISSION
// ============================================================

/**
 * Submit a deck to Firestore.
 * @param {Object} deckData - { title, author, description, cards, sideboard, colorIdentity, colorCode, tags, badges }
 */
export async function submitDeck(deckData) {
  var user = await ensureAuth();

  var cardCount = 0;
  for (var i = 0; i < deckData.cards.length; i++) {
    cardCount += deckData.cards[i].qty;
  }

  function mapCards(arr) {
    return (arr || []).map(function(c) {
      return { qty: c.qty, name: c.name, set: c.set || null, scryfallImg: c.scryfallImg || '', typeLine: c.typeLine || '' };
    });
  }

  var deckDoc = {
    title: deckData.title,
    author: deckData.author,
    authorUid: user.uid,
    description: deckData.description || '',
    cards: mapCards(deckData.cards),
    sideboard: mapCards(deckData.sideboard),
    cardCount: cardCount,
    colorIdentity: deckData.colorIdentity,
    colorCode: deckData.colorCode,
    tags: deckData.tags || [],
    badges: deckData.badges || [],
    voteCount: 0,
    status: 'published',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  };

  var docRef = await addDoc(collection(db, 'decks'), deckDoc);
  return docRef.id;
}

// ============================================================
// VOTING
// ============================================================

/**
 * Toggle vote on a deck. Returns { voted: boolean, newCount: number }.
 */
export async function toggleVote(deckId) {
  var user = await ensureAuth();
  var voteId = deckId + '__' + user.uid;
  var voteRef = doc(db, 'votes', voteId);
  var deckRef = doc(db, 'decks', deckId);

  var voteSnap = await getDoc(voteRef);

  if (voteSnap.exists()) {
    // Unvote
    await deleteDoc(voteRef);
    await updateDoc(deckRef, { voteCount: increment(-1), updatedAt: serverTimestamp() });
    var deckSnap = await getDoc(deckRef);
    return { voted: false, newCount: deckSnap.data().voteCount };
  } else {
    // Vote
    await setDoc(voteRef, {
      deckId: deckId,
      oderId: user.uid,
      createdAt: serverTimestamp()
    });
    await updateDoc(deckRef, { voteCount: increment(1), updatedAt: serverTimestamp() });
    var deckSnap2 = await getDoc(deckRef);
    return { voted: true, newCount: deckSnap2.data().voteCount };
  }
}

/**
 * Check which decks the current user has voted on.
 * @param {string[]} deckIds - Array of deck IDs to check
 * @returns {Set<string>} Set of deck IDs that the user has voted on
 */
export async function getVotedDecks(deckIds) {
  var voted = new Set();
  if (!currentUser || !deckIds.length) return voted;

  for (var i = 0; i < deckIds.length; i++) {
    var voteId = deckIds[i] + '__' + currentUser.uid;
    var snap = await getDoc(doc(db, 'votes', voteId));
    if (snap.exists()) voted.add(deckIds[i]);
  }
  return voted;
}

// ============================================================
// DECK DOWNLOAD (.dck format)
// ============================================================

/**
 * Generate a .dck file content string from a deck object.
 * Forge .dck format: [metadata]\nName=...\n\n[Main]\ncount cardname\n...
 */
export function generateDckContent(deck) {
  var lines = [];
  lines.push('[metadata]');
  lines.push('Name=' + deck.title);
  if (deck.description) lines.push('Comment=' + deck.description);
  lines.push('');
  lines.push('[Main]');
  if (deck.cards) {
    for (var i = 0; i < deck.cards.length; i++) {
      var card = deck.cards[i];
      var line = card.qty + ' ' + card.name;
      if (card.set) line += '|' + card.set + '|1';
      lines.push(line);
    }
  }
  if (deck.sideboard && deck.sideboard.length > 0) {
    lines.push('');
    lines.push('[Sideboard]');
    for (var j = 0; j < deck.sideboard.length; j++) {
      var sb = deck.sideboard[j];
      var sbLine = sb.qty + ' ' + sb.name;
      if (sb.set) sbLine += '|' + sb.set + '|1';
      lines.push(sbLine);
    }
  }
  return lines.join('\n');
}

/**
 * Trigger a .dck file download in the browser.
 */
export function downloadDeck(deck) {
  var content = generateDckContent(deck);
  var slug = deck.title.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
  var filename = (slug || 'deck') + '.dck';
  var blob = new Blob([content], { type: 'text/plain' });
  var url = URL.createObjectURL(blob);
  var a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// ============================================================
// DECK FETCHING
// ============================================================

/**
 * Fetch decks with filters, sorting, and pagination.
 * @param {Object} opts - { colors?, tag?, badge?, sortBy?, pageSize?, lastDoc? }
 * @returns { decks: [], lastDoc: DocumentSnapshot|null, hasMore: boolean }
 */
export async function fetchDecks(opts) {
  opts = opts || {};
  var pageSize = opts.pageSize || 12;
  var colors = opts.colors || [];

  var constraints = [where('status', '==', 'published')];

  // Firestore allows only one array-contains per query.
  // Use it for the first color if no tag/badge also needs it.
  var usedArrayContains = false;
  var clientColors = colors.slice();

  if (colors.length > 0 && !opts.tag && !opts.badge) {
    constraints.push(where('colorIdentity', 'array-contains', colors[0]));
    usedArrayContains = true;
    clientColors = colors.slice(1);
  }

  if (opts.tag) {
    constraints.push(where('tags', 'array-contains', opts.tag));
    usedArrayContains = true;
  }
  if (opts.badge && !usedArrayContains) {
    constraints.push(where('badges', 'array-contains', opts.badge));
    usedArrayContains = true;
  }

  // Sort
  var sortField = 'createdAt';
  var sortDir = 'desc';
  if (opts.sortBy === 'votes') {
    sortField = 'voteCount';
    sortDir = 'desc';
  } else if (opts.sortBy === 'alpha') {
    sortField = 'title';
    sortDir = 'asc';
  }
  constraints.push(orderBy(sortField, sortDir));

  // Over-fetch when client-side filtering is needed
  var needsClientFilter = clientColors.length > 0
    || (opts.badge && opts.tag);
  var fetchSize = needsClientFilter ? pageSize * 5 : pageSize;
  constraints.push(limit(fetchSize + 1));

  if (opts.lastDoc) {
    constraints.push(startAfter(opts.lastDoc));
  }

  var q = query(collection(db, 'decks'), ...constraints);
  var snap = await getDocs(q);

  var decks = [];
  var lastVisible = null;
  var hasMore = false;

  snap.forEach(function(docSnap) {
    var data = docSnap.data();

    // Client-side color filter: deck must contain ALL selected colors
    if (clientColors.length > 0) {
      var ci = data.colorIdentity || [];
      for (var i = 0; i < clientColors.length; i++) {
        if (ci.indexOf(clientColors[i]) < 0) return;
      }
    }

    // Client-side badge filter when it couldn't go in the query
    if (opts.badge && opts.tag) {
      if (!data.badges || data.badges.indexOf(opts.badge) < 0) return;
    }

    if (decks.length < pageSize) {
      decks.push({ id: docSnap.id, ...data });
      lastVisible = docSnap;
    } else {
      hasMore = true;
    }
  });

  return { decks: decks, lastDoc: lastVisible, hasMore: hasMore };
}

/**
 * Fetch a single deck by ID.
 */
export async function fetchDeck(deckId) {
  var snap = await getDoc(doc(db, 'decks', deckId));
  if (!snap.exists()) return null;
  return { id: snap.id, ...snap.data() };
}

/** Default tags/badges used when registry doc doesn't exist yet */
var defaultRegistry = {
  archetypes: ['aggro','control','combo','midrange','tempo','ramp','tribal','mill','burn','reanimator','prison','toolbox'],
  badges: ['boss-killer','budget','meme','competitive','beginner-friendly','jank','spicy']
};

/**
 * Fetch the tags/badges registry.
 */
export async function fetchTagRegistry() {
  try {
    var snap = await getDoc(doc(db, 'tags', 'registry'));
    if (!snap.exists()) {
      return { archetypes: defaultRegistry.archetypes.slice(), badges: defaultRegistry.badges.slice() };
    }
    return snap.data();
  } catch(e) {
    return { archetypes: defaultRegistry.archetypes.slice(), badges: defaultRegistry.badges.slice() };
  }
}

/**
 * Add a new tag or badge to the registry if it doesn't already exist.
 * @param {string} type - 'archetypes' or 'badges'
 * @param {string} value - The tag/badge to add (will be lowercased and slugified)
 */
export async function addToRegistry(type, value) {
  if (type !== 'archetypes' && type !== 'badges') return;

  var slug = value.toLowerCase().trim().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');
  if (!slug || slug.length > 30) return;

  await ensureAuth();

  var regRef = doc(db, 'tags', 'registry');
  var snap = await getDoc(regRef);

  if (!snap.exists()) {
    // Create registry with defaults + new value
    var data = { archetypes: defaultRegistry.archetypes.slice(), badges: defaultRegistry.badges.slice() };
    if (data[type].indexOf(slug) === -1) data[type].push(slug);
    await setDoc(regRef, data);
  } else {
    var existing = snap.data()[type] || [];
    if (existing.indexOf(slug) === -1) {
      existing.push(slug);
      var update = {};
      update[type] = existing;
      await updateDoc(regRef, update);
    }
  }

  return slug;
}

// ============================================================
// IMAGE URL HELPERS
// ============================================================

/**
 * Convert a Scryfall small image URL to normal size.
 * small: https://cards.scryfall.io/small/front/a/b/uuid.jpg
 * normal: https://cards.scryfall.io/normal/front/a/b/uuid.jpg
 */
export function getNormalImgUrl(smallUrl) {
  if (!smallUrl) return '';
  return smallUrl.replace('/small/', '/normal/');
}

// ============================================================
// CARD TYPE GROUPING
// ============================================================

var typeOrder = ['Creature', 'Instant', 'Sorcery', 'Enchantment', 'Artifact', 'Planeswalker', 'Land', 'Other'];

/**
 * Extract the primary type category from a Scryfall type_line.
 * e.g. "Legendary Creature — Dragon" → "Creature"
 */
function getPrimaryType(typeLine) {
  if (!typeLine) return 'Other';
  var t = typeLine.split('—')[0].trim().split('//')[0].trim();
  for (var i = 0; i < typeOrder.length - 1; i++) {
    if (t.indexOf(typeOrder[i]) >= 0) return typeOrder[i];
  }
  return 'Other';
}

/**
 * Group cards by their primary type.
 * @param {Array} cards - [{ qty, name, typeLine, ... }]
 * @returns {Array} [{ type: string, cards: [...], totalQty: number }] sorted by typeOrder
 */
export function groupCardsByType(cards) {
  var groups = {};
  for (var i = 0; i < cards.length; i++) {
    var type = getPrimaryType(cards[i].typeLine);
    if (!groups[type]) groups[type] = { type: type, cards: [], totalQty: 0 };
    groups[type].cards.push(cards[i]);
    groups[type].totalQty += cards[i].qty;
  }
  // Sort by typeOrder
  var result = [];
  for (var j = 0; j < typeOrder.length; j++) {
    if (groups[typeOrder[j]]) result.push(groups[typeOrder[j]]);
  }
  return result;
}

// ============================================================
// RENDERING HELPERS
// ============================================================

/**
 * Escape HTML special characters to prevent XSS.
 * All user-provided or API-provided strings must be escaped before insertion into HTML.
 */
export function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Mana color names for title attributes */
var manaNames = { W: 'White', U: 'Blue', B: 'Black', R: 'Red', G: 'Green' };

/**
 * Render color identity pips as HTML using Mana font icons.
 * @param {string[]} colorIdentity - e.g. ["W","U","R"]
 * @returns {string} HTML string
 */
export function renderColorPips(colorIdentity) {
  if (!colorIdentity || !colorIdentity.length) {
    return '<i class="ms ms-c ms-cost ms-shadow" title="Colorless" style="font-size:13px;margin:0 1px"></i>';
  }
  var html = '';
  for (var i = 0; i < colorIdentity.length; i++) {
    var c = colorIdentity[i].toLowerCase();
    var name = manaNames[colorIdentity[i]] || colorIdentity[i];
    html += '<i class="ms ms-' + c + ' ms-cost ms-shadow" title="' + name + '" style="font-size:13px;margin:0 1px"></i>';
  }
  return html;
}

/**
 * Render color identity pips using safe DOM methods (no innerHTML).
 * Appends <i> mana font elements to the given parent.
 */
export function renderColorPipsDom(colorIdentity, parent) {
  if (!colorIdentity || !colorIdentity.length) {
    var icon = document.createElement('i');
    icon.className = 'ms ms-c ms-cost ms-shadow';
    icon.title = 'Colorless';
    icon.style.cssText = 'font-size:13px;margin:0 1px';
    parent.appendChild(icon);
    return;
  }
  for (var i = 0; i < colorIdentity.length; i++) {
    var icon = document.createElement('i');
    icon.className = 'ms ms-' + colorIdentity[i].toLowerCase() + ' ms-cost ms-shadow';
    icon.title = manaNames[colorIdentity[i]] || colorIdentity[i];
    icon.style.cssText = 'font-size:13px;margin:0 1px';
    parent.appendChild(icon);
  }
}

/**
 * Render a tag label using safe DOM methods. Appends to parent.
 */
export function renderTagDom(tag, parent) {
  var span = document.createElement('span');
  span.style.cssText = 'background:#333366;color:#AACCFF;padding:1px 5px;font-size:10px;margin:0 2px;border:1px solid #555588';
  span.textContent = tag;
  parent.appendChild(span);
}

/**
 * Render a badge label using safe DOM methods. Appends to parent.
 */
export function renderBadgeDom(badge, parent) {
  var colors = {
    'boss-killer': '#FF4444', 'budget': '#44AA44', 'meme': '#FF66FF',
    'competitive': '#FFD700', 'beginner-friendly': '#44AAFF', 'jank': '#FF8800', 'spicy': '#FF4400'
  };
  var bg = colors[badge] || '#666688';
  var span = document.createElement('span');
  span.style.cssText = 'background:' + bg + ';color:#fff;padding:1px 5px;font-size:10px;margin:0 2px;border-radius:2px';
  span.textContent = badge.replace(/-/g, ' ');
  parent.appendChild(span);
}

/**
 * Render a badge label (returns HTML string, uses escapeHtml).
 */
export function renderBadge(badge) {
  var colors = {
    'boss-killer': '#FF4444',
    'budget': '#44AA44',
    'meme': '#FF66FF',
    'competitive': '#FFD700',
    'beginner-friendly': '#44AAFF',
    'jank': '#FF8800',
    'spicy': '#FF4400'
  };
  var bg = colors[badge] || '#666688';
  return '<span style="background:' + bg + ';color:#fff;padding:1px 5px;font-size:10px;margin:0 2px;border-radius:2px">' +
    escapeHtml(badge.replace(/-/g, ' ')) + '</span>';
}

/**
 * Render a tag label.
 */
export function renderTag(tag) {
  return '<span style="background:#333366;color:#AACCFF;padding:1px 5px;font-size:10px;margin:0 2px;border:1px solid #555588">' +
    escapeHtml(tag) + '</span>';
}

/**
 * Format a Firestore timestamp for display.
 */
export function formatDate(ts) {
  if (!ts) return '';
  var date = ts.toDate ? ts.toDate() : new Date(ts);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

/**
 * Build the color code string from selected color buttons.
 * @param {string[]} selectedColors - e.g. ["W","R"]
 * @returns {string} Sorted color code e.g. "RW"
 */
export function buildColorCode(selectedColors) {
  var order = ['W','U','B','R','G'];
  return order.filter(function(c) { return selectedColors.indexOf(c) >= 0; }).join('');
}

// ============================================================
// COMMENTS
// ============================================================

/**
 * Fetch comments for a deck, ordered by newest first.
 * @param {string} deckId
 * @returns {Promise<Array>} [{ id, text, authorName, authorUid, createdAt }]
 */
export async function fetchComments(deckId) {
  var commentsRef = collection(db, 'decks', deckId, 'comments');
  var q = query(commentsRef, orderBy('createdAt', 'desc'), limit(100));
  var snap = await getDocs(q);
  return snap.docs.map(function(d) {
    var data = d.data();
    data.id = d.id;
    return data;
  });
}

/**
 * Add a comment to a deck. Rate-limited: 1 comment per 2 minutes per user.
 * @param {string} deckId
 * @param {string} text - Comment text (10-500 chars)
 * @param {string} authorName - Display name (1-50 chars)
 * @returns {Promise<object>} The new comment data
 */
export async function addComment(deckId, text, authorName) {
  var user = await ensureAuth();
  if (!user) throw new Error('Authentication required');

  text = text.trim();
  authorName = authorName.trim();
  if (text.length < 3 || text.length > 500) throw new Error('Comment must be 3-500 characters');
  if (authorName.length < 1 || authorName.length > 50) throw new Error('Name must be 1-50 characters');

  // Rate limit: check for recent comments by this user (any deck)
  var recentQuery = query(
    collection(db, 'decks', deckId, 'comments'),
    where('authorUid', '==', user.uid),
    orderBy('createdAt', 'desc'),
    limit(1)
  );
  var recentSnap = await getDocs(recentQuery);
  if (recentSnap.docs.length > 0) {
    var lastComment = recentSnap.docs[0].data();
    if (lastComment.createdAt) {
      var lastTime = lastComment.createdAt.toDate ? lastComment.createdAt.toDate() : new Date(lastComment.createdAt);
      var elapsed = Date.now() - lastTime.getTime();
      if (elapsed < 120000) {
        var waitSec = Math.ceil((120000 - elapsed) / 1000);
        throw new Error('Please wait ' + waitSec + ' seconds before commenting again');
      }
    }
  }

  var commentData = {
    text: text,
    authorName: authorName,
    authorUid: user.uid,
    createdAt: serverTimestamp()
  };

  var commentsRef = collection(db, 'decks', deckId, 'comments');
  var docRef = await addDoc(commentsRef, commentData);
  commentData.id = docRef.id;
  commentData.createdAt = { toDate: function() { return new Date(); } };
  return commentData;
}

/**
 * Create a card hover tooltip element. Call once per page, reuse the element.
 */
export function createTooltip() {
  var tip = document.createElement('div');
  tip.id = 'card-tooltip';
  tip.style.cssText = 'display:none;position:fixed;z-index:9999;pointer-events:none;' +
    'border:2px solid #663399;background:#000;padding:2px;box-shadow:0 0 10px rgba(0,0,0,0.8)';
  var img = document.createElement('img');
  img.style.cssText = 'display:block;width:190px;height:265px';
  tip.appendChild(img);
  document.body.appendChild(tip);

  document.addEventListener('mousemove', function(e) {
    if (tip.style.display === 'none') return;
    var x = e.clientX + 15;
    var y = e.clientY - 100;
    // Keep on screen
    if (x + 200 > window.innerWidth) x = e.clientX - 205;
    if (y < 5) y = 5;
    if (y + 275 > window.innerHeight) y = window.innerHeight - 280;
    tip.style.left = x + 'px';
    tip.style.top = y + 'px';
  });

  return tip;
}

/**
 * Show card tooltip on a hover-target element.
 * Usage: attach to mouseenter/mouseleave on card name elements with data-img attribute.
 */
export function setupCardHovers(container) {
  var tip = document.getElementById('card-tooltip') || createTooltip();
  var img = tip.querySelector('img');

  container.addEventListener('mouseenter', function(e) {
    var target = e.target.closest('[data-img]');
    if (!target) return;
    var imgUrl = target.getAttribute('data-img');
    // Only allow Scryfall CDN image URLs
    if (imgUrl && imgUrl.match(/^https:\/\/cards\.scryfall\.io\//)) {
      img.src = imgUrl;
      tip.style.display = 'block';
    }
  }, true);

  container.addEventListener('mouseleave', function(e) {
    var target = e.target.closest('[data-img]');
    if (!target) return;
    tip.style.display = 'none';
  }, true);
}
