(() => {
  "use strict";
  const PRISTINE = "<!doctype html>\n" + document.documentElement.outerHTML;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const DESIGN = document.documentElement.dataset.design || "zine-merah";
  const DEF_MODE = document.documentElement.dataset.mode === "port" ? "port" : "land";
  const book = $("#book");
  const pages = $$(".page", book);

  let framed = false;
  try { framed = window.self !== window.top; } catch (e) { framed = true; }
  if (framed) document.body.classList.add("framed");

  /* ---------- keys & defaults ---------- */
  const defaults = new Map();
  const texts = [], photos = [], checks = [];
  const tidy = s => s.replace(/\r/g, "").replace(/[ \t]+\n/g, "\n").replace(/\n[ \t]+/g, "\n").trim();
  pages.forEach(p => {
    const id = p.dataset.id;
    $$("[e]", p).forEach((el, i) => { el.dataset.k = id + "-t" + i; const t = tidy(el.textContent); el.textContent = t; defaults.set(el, t); texts.push(el); });
    $$(".ph", p).forEach((el, i) => { el.dataset.k = id + "-p" + i; photos.push(el); });
    $$(".ck", p).forEach((el, i) => { el.dataset.k = id + "-c" + i; checks.push(el); });
  });

  /* ---------- storage ---------- */
  function openDb() {
    return new Promise((res, rej) => {
      const r = indexedDB.open("zine-kita", 1);
      r.onupgradeneeded = () => r.result.createObjectStore("kv");
      r.onsuccess = () => res(r.result);
      r.onerror = () => rej(r.error);
    });
  }
  async function dbGet(k) {
    try { const db = await openDb(); return await new Promise((res, rej) => { const q = db.transaction("kv").objectStore("kv").get(k); q.onsuccess = () => res(q.result); q.onerror = () => rej(q.error); }); }
    catch (e) { return null; }
  }
  async function dbSet(k, v) {
    try { const db = await openDb(); await new Promise((res, rej) => { const t = db.transaction("kv", "readwrite"); t.objectStore("kv").put(v, k); t.oncomplete = res; t.onerror = () => rej(t.error); }); return true; }
    catch (e) { return false; }
  }

  let embedded = null;
  try { embedded = JSON.parse($("#sb-data").textContent); } catch (e) { embedded = null; }
  const newId = () => Math.random().toString(36).slice(2, 10);
  let state = { sid: newId(), texts: {}, photos: {}, checks: {}, mode: innerWidth < 720 ? "port" : DEF_MODE, edit: !embedded };
  let storeKey = DESIGN === "zine-merah" ? "state" : DESIGN + ":state";
  if (embedded) document.body.classList.add("shared");

  const statusEl = $("#status");
  let saveTimer = null;
  function save() {
    statusEl.textContent = "Menyimpan…";
    clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      const ok = await dbSet(storeKey, state);
      statusEl.textContent = ok ? "Tersimpan" : "Tidak tersimpan";
    }, 350);
  }
  const toastEl = $("#toast");
  let toastTimer = null;
  function toast(msg) { toastEl.textContent = msg; toastEl.classList.add("show"); clearTimeout(toastTimer); toastTimer = setTimeout(() => toastEl.classList.remove("show"), 2600); }

  /* ---------- render ---------- */
  const CAM = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 8h3l1.6-2.2h6.8L17 8h3v11H4z"/><circle cx="12" cy="13.2" r="3.6"/></svg>';
  function renderPhoto(ph) {
    const d = state.photos[ph.dataset.k];
    ph.classList.toggle("has", !!d);
    ph.textContent = "";
    if (d) {
      const img = new Image();
      img.alt = ""; img.draggable = false; img.src = d.src;
      img.style.objectPosition = (d.x ?? 50) + "% " + (d.y ?? 50) + "%";
      const rm = document.createElement("button");
      rm.type = "button"; rm.className = "ph-rm edit-only"; rm.setAttribute("aria-label", "Hapus foto"); rm.textContent = "×";
      ph.append(img, rm);
    } else {
      ph.insertAdjacentHTML("beforeend", '<span class="ph-empty">' + CAM + '</span>');
    }
  }
  const esc = t => t.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  const rawOf = el => { const v = state.texts[el.dataset.k]; return v != null ? v : defaults.get(el); };
  function renderText(el) {
    const raw = rawOf(el);
    if (el.hasAttribute("rich") && document.activeElement !== el) el.innerHTML = esc(raw).replace(/\*([^*\n]+)\*/g, "<b>$1</b>");
    else el.textContent = raw;
  }
  book.addEventListener("focusin", e => { const el = e.target.closest && e.target.closest("[rich]"); if (el && state.edit) el.textContent = rawOf(el); });
  book.addEventListener("focusout", e => { const el = e.target.closest && e.target.closest("[rich]"); if (el) setTimeout(() => renderText(el), 0); });
  function renderAll() {
    texts.forEach(renderText);
    photos.forEach(renderPhoto);
    checks.forEach(el => { const on = !!state.checks[el.dataset.k]; el.classList.toggle("on", on); el.setAttribute("aria-pressed", on); });
  }

  /* ---------- layout ---------- */
  const pageStyle = $("#page-size");
  function layout() {
    const m = state.mode;
    const groups = [];
    if (m === "land") for (let i = 0; i < pages.length; i += 2) groups.push(pages.slice(i, i + 2));
    else pages.forEach(p => groups.push([p]));
    const frag = document.createDocumentFragment();
    groups.forEach(g => {
      const wrap = document.createElement("div"); wrap.className = "sheet-wrap";
      const lab = document.createElement("div"); lab.className = "sheet-label";
      const nums = g.map(p => pages.indexOf(p) + 1);
      const n = document.createElement("span"); n.className = "lab-n"; n.textContent = "hal. " + nums.join(" & ");
      const t = document.createElement("span"); t.className = "lab-t"; t.textContent = g.map(p => p.dataset.label).join(" · ");
      lab.append(n, t);
      const sheet = document.createElement("div"); sheet.className = "sheet " + m;
      g.forEach(p => sheet.append(p));
      wrap.append(lab, sheet); frag.append(wrap);
    });
    book.replaceChildren(frag);
    pageStyle.textContent = "@page{size:A4 " + (m === "land" ? "landscape" : "portrait") + ";margin:0}";
    $("#m-land").setAttribute("aria-pressed", m === "land");
    $("#m-port").setAttribute("aria-pressed", m === "port");
  }

  /* ---------- edit mode ---------- */
  let plainOnly = null;
  function setEdit(on) {
    state.edit = on;
    document.body.classList.toggle("editing", on);
    $("#t-edit").setAttribute("aria-pressed", on);
    $("#t-edit").textContent = on ? "Selesai edit" : "Mode edit";
    if (!on) { stopPan(); $("#menu").hidden = true; }
    texts.forEach(el => {
      if (on) {
        if (plainOnly === null) { el.contentEditable = "plaintext-only"; plainOnly = el.contentEditable === "plaintext-only"; }
        el.contentEditable = plainOnly ? "plaintext-only" : "true";
        el.spellcheck = false;
      } else el.removeAttribute("contenteditable");
    });
    photos.forEach(ph => {
      if (on) { ph.tabIndex = 0; ph.setAttribute("role", "button"); ph.setAttribute("aria-label", "Pilih foto"); }
      else { ph.removeAttribute("tabindex"); ph.removeAttribute("role"); ph.removeAttribute("aria-label"); }
    });
  }

  book.addEventListener("input", e => {
    const el = e.target.closest("[e]");
    if (!el) return;
    state.texts[el.dataset.k] = el.innerText.replace(/\n$/, "");
    save();
  });
  book.addEventListener("paste", e => {
    const el = e.target.closest("[e]");
    if (!el || plainOnly) return;
    e.preventDefault();
    const txt = (e.clipboardData || window.clipboardData).getData("text/plain");
    document.execCommand("insertText", false, txt);
  });

  /* ---------- photos ---------- */
  const isHeic = f => /image\/hei[cf]/i.test(f.type) || /\.hei[cf]$/i.test(f.name || "");
  let heicLib = null;
  function loadHeic() {
    const urls = ["https://cdn.jsdelivr.net/npm/heic2any@0.0.4/dist/heic2any.min.js", "https://unpkg.com/heic2any@0.0.4/dist/heic2any.min.js"];
    const tryLoad = i => new Promise((res, rej) => {
      if (i >= urls.length) { rej(new Error("heic")); return; }
      const s = document.createElement("script");
      s.src = urls[i];
      s.onload = () => window.heic2any ? res(window.heic2any) : rej(new Error("heic"));
      s.onerror = () => { s.remove(); tryLoad(i + 1).then(res, rej); };
      document.head.append(s);
    });
    if (!heicLib) heicLib = tryLoad(0).catch(e => { heicLib = null; throw e; });
    return heicLib;
  }
  async function heicToJpeg(file) {
    const conv = await loadHeic();
    const out = await conv({ blob: file, toType: "image/jpeg", quality: 0.9 });
    return Array.isArray(out) ? out[0] : out;
  }
  async function draw(blob) {
    const max = 1600;
    let src = null, w = 0, h = 0, url = null;
    try {
      if (window.createImageBitmap) {
        try { src = await createImageBitmap(blob, { imageOrientation: "from-image" }); w = src.width; h = src.height; } catch (e) { src = null; }
      }
      if (!src) {
        url = URL.createObjectURL(blob);
        const img = new Image(); img.src = url; await img.decode();
        src = img; w = img.naturalWidth; h = img.naturalHeight;
      }
      if (!w || !h) throw new Error("empty");
      const s = Math.min(1, max / Math.max(w, h));
      const c = document.createElement("canvas"); c.width = Math.round(w * s); c.height = Math.round(h * s);
      const ctx = c.getContext("2d");
      ctx.fillStyle = "#fff"; ctx.fillRect(0, 0, c.width, c.height);
      ctx.drawImage(src, 0, 0, c.width, c.height);
      if (src.close) src.close();
      return c.toDataURL("image/jpeg", 0.85);
    } finally { if (url) URL.revokeObjectURL(url); }
  }
  async function compress(file) {
    if (isHeic(file)) {
      try { return await draw(file); } catch (e) { return await draw(await heicToJpeg(file)); }
    }
    try { return await draw(file); }
    catch (e) { return await draw(await heicToJpeg(file)); }
  }
  const imageFiles = list => Array.from(list || []).filter(f => /^image\//.test(f.type) || isHeic(f) || !f.type);

  let busy = false;
  async function placeFiles(files, first) {
    files = imageFiles(files);
    if (!files.length) return;
    if (busy) { toast("Tunggu, foto sebelumnya masih diproses."); return; }
    busy = true;
    const empties = photos.filter(ph => ph !== first && !state.photos[ph.dataset.k]);
    const targets = first ? [first, ...empties] : empties;
    let n = 0, failed = 0, i = 0;
    for (const f of files) {
      const ph = targets[i]; if (!ph) break;
      statusEl.textContent = "Memproses foto " + (n + failed + 1) + "/" + Math.min(files.length, targets.length) + "…";
      ph.classList.add("loading");
      try { state.photos[ph.dataset.k] = { src: await compress(f), x: 50, y: 50 }; renderPhoto(ph); n++; i++; }
      catch (e) { failed++; }
      ph.classList.remove("loading");
    }
    busy = false;
    save();
    const left = files.length - n - failed;
    if (failed) toast(failed + " foto tidak bisa dibuka. Foto HEIC perlu koneksi internet; atau simpan ulang sebagai JPG.");
    else if (left > 0) toast(n + " foto dimasukkan. " + left + " foto tidak muat, semua bingkai sudah terisi.");
    else if (files.length > 1) toast(n + " foto dimasukkan.");
  }

  let target = null, lastPh = null;
  const fileOne = $("#file-one"), fileCam = $("#file-cam"), fileMany = $("#file-many");
  const openInput = inp => { inp.value = ""; inp.click(); };
  fileOne.addEventListener("change", () => placeFiles(fileOne.files, target));
  fileCam.addEventListener("change", () => placeFiles(fileCam.files, target));
  fileMany.addEventListener("change", () => placeFiles(fileMany.files, null));
  $("#b-many").addEventListener("click", () => openInput(fileMany));

  /* menu */
  const menu = $("#menu");
  function openMenu(ph) {
    target = lastPh = ph;
    const has = !!state.photos[ph.dataset.k];
    $$("[data-has]", menu).forEach(b => { b.hidden = !has; });
    menu.hidden = false;
    $(".mbtn", menu).focus();
  }
  function closeMenu() { menu.hidden = true; if (target) try { target.focus({ preventScroll: true }); } catch (e) {} }
  menu.addEventListener("click", e => {
    if (e.target === menu) { closeMenu(); return; }
    const b = e.target.closest("[data-act]"); if (!b) return;
    const act = b.dataset.act, ph = target;
    menu.hidden = true;
    if (act === "gallery") openInput(fileOne);
    else if (act === "camera") openInput(fileCam);
    else if (act === "pan") startPan(ph);
    else if (act === "remove") { delete state.photos[ph.dataset.k]; renderPhoto(ph); save(); }
  });
  document.addEventListener("keydown", e => {
    if (e.key === "Escape") { if (!menu.hidden) closeMenu(); else if (panPh) stopPan(); }
  });

  /* pan mode (touch needs it so the page still scrolls) */
  let panPh = null;
  const panDone = $("#pan-done");
  function startPan(ph) { stopPan(); panPh = ph; ph.classList.add("panning"); panDone.hidden = false; }
  function stopPan() { if (panPh) panPh.classList.remove("panning"); panPh = null; panDone.hidden = true; }
  panDone.addEventListener("click", () => { stopPan(); save(); });

  let drag = null;
  book.addEventListener("pointerdown", e => {
    if (!state.edit || e.button > 0) return;
    const ph = e.target.closest(".ph");
    if (!ph || e.target.closest(".ph-rm")) return;
    const img = ph.querySelector("img");
    const d = state.photos[ph.dataset.k];
    const canPan = !!img && (e.pointerType === "mouse" || ph === panPh);
    drag = { ph, img, canPan, x: e.clientX, y: e.clientY, moved: false, sx: d ? d.x ?? 50 : 50, sy: d ? d.y ?? 50 : 50 };
    if (canPan) { e.preventDefault(); try { ph.setPointerCapture(e.pointerId); } catch (_) {} }
  });
  book.addEventListener("pointermove", e => {
    if (!drag) return;
    const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
    if (!drag.moved && Math.hypot(dx, dy) < 6) return;
    drag.moved = true;
    if (!drag.canPan) return;
    const { ph, img } = drag;
    ph.classList.add("grabbing");
    const fw = ph.clientWidth, fh = ph.clientHeight, nw = img.naturalWidth || 1, nh = img.naturalHeight || 1;
    const s = Math.max(fw / nw, fh / nh), ox = nw * s - fw, oy = nh * s - fh;
    const clamp = v => Math.max(0, Math.min(100, v));
    const d = state.photos[ph.dataset.k];
    if (ox > 1) d.x = clamp(drag.sx - dx / ox * 100);
    if (oy > 1) d.y = clamp(drag.sy - dy / oy * 100);
    img.style.objectPosition = d.x + "% " + d.y + "%";
  });
  function endDrag(e) {
    if (!drag) return;
    const dd = drag; drag = null;
    dd.ph.classList.remove("grabbing");
    if (dd.moved) { suppressUntil = Date.now() + 400; if (dd.canPan) save(); }
  }
  let suppressUntil = 0;
  book.addEventListener("pointerup", endDrag);
  book.addEventListener("pointercancel", endDrag);
  book.addEventListener("keydown", e => {
    const ph = e.target.closest(".ph");
    if (state.edit && ph && e.target === ph && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); openMenu(ph); }
  });
  book.addEventListener("focusin", e => { const ph = e.target.closest && e.target.closest(".ph"); if (ph) lastPh = ph; });

  /* drag & drop files */
  let dropPh = null;
  const hasFiles = e => e.dataTransfer && Array.from(e.dataTransfer.types || []).includes("Files");
  book.addEventListener("dragover", e => {
    if (!state.edit || !hasFiles(e)) return;
    e.preventDefault(); e.dataTransfer.dropEffect = "copy";
    const ph = e.target.closest(".ph");
    if (ph !== dropPh) { if (dropPh) dropPh.classList.remove("drop"); dropPh = ph; if (ph) ph.classList.add("drop"); }
  });
  book.addEventListener("dragleave", e => { if (dropPh && !book.contains(e.relatedTarget)) { dropPh.classList.remove("drop"); dropPh = null; } });
  book.addEventListener("drop", e => {
    if (!state.edit || !hasFiles(e)) return;
    e.preventDefault();
    const ph = e.target.closest(".ph");
    if (dropPh) dropPh.classList.remove("drop"); dropPh = null;
    placeFiles(e.dataTransfer.files, ph);
  });
  window.addEventListener("dragover", e => { if (hasFiles(e)) e.preventDefault(); });
  window.addEventListener("drop", e => { if (hasFiles(e) && !book.contains(e.target)) { e.preventDefault(); if (state.edit) placeFiles(e.dataTransfer.files, null); } });

  /* paste an image (Ctrl/Cmd+V) */
  document.addEventListener("paste", e => {
    if (!state.edit || !e.clipboardData) return;
    const files = Array.from(e.clipboardData.items || []).filter(it => it.kind === "file" && /^image\//.test(it.type)).map(it => it.getAsFile()).filter(Boolean);
    if (!files.length) return;
    e.preventDefault(); e.stopPropagation();
    placeFiles(files, lastPh && document.contains(lastPh) ? lastPh : null);
  }, true);

  book.addEventListener("click", e => {
    const rm = e.target.closest(".ph-rm");
    if (rm) { const ph = rm.closest(".ph"); if (ph === panPh) stopPan(); delete state.photos[ph.dataset.k]; renderPhoto(ph); save(); return; }
    const ph = e.target.closest(".ph");
    if (ph && state.edit) {
      if (Date.now() < suppressUntil || ph === panPh) return;
      if (panPh) { stopPan(); save(); }
      openMenu(ph); return;
    }
    const ck = e.target.closest(".ck");
    if (ck) { const k = ck.dataset.k; state.checks[k] = !state.checks[k]; ck.classList.toggle("on", state.checks[k]); ck.setAttribute("aria-pressed", state.checks[k]); save(); }
  });

  /* ---------- toolbar ---------- */
  $("#m-land").addEventListener("click", () => { state.mode = "land"; layout(); save(); });
  $("#m-port").addEventListener("click", () => { state.mode = "port"; layout(); save(); });
  $("#t-edit").addEventListener("click", () => { setEdit(!state.edit); save(); });
  $("#b-print").addEventListener("click", () => {
    if (document.activeElement) document.activeElement.blur();
    setTimeout(() => window.print(), 50);
  });
  const resetBtn = $("#b-reset");
  let resetArmed = null;
  resetBtn.addEventListener("click", () => {
    if (!resetArmed) {
      resetBtn.textContent = "Yakin? Klik lagi";
      resetArmed = setTimeout(() => { resetArmed = null; resetBtn.textContent = "Reset"; }, 3500);
      return;
    }
    clearTimeout(resetArmed); resetArmed = null; resetBtn.textContent = "Reset";
    state.texts = {}; state.photos = {}; state.checks = {};
    renderAll(); save(); toast("Semua foto dan tulisan kembali ke awal.");
  });

  function download(blob, name) {
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob); a.download = name;
    document.body.append(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 4000);
  }
  $("#b-share").addEventListener("click", async () => {
    const data = Object.assign({}, state, { edit: false });
    const json = JSON.stringify(data).replace(/</g, "\\u003c");
    const open = '<scr' + 'ipt id="sb-data" type="application/json">';
    const a = PRISTINE.indexOf(open), b = PRISTINE.indexOf("</scr" + "ipt>", a);
    if (a < 0 || b < 0) { toast("File tidak bisa dibuat."); return; }
    const html = PRISTINE.slice(0, a + open.length) + json + PRISTINE.slice(b);
    const name = DESIGN + ".html";
    const blob = new Blob([html], { type: "text/html" });
    try {
      const file = new File([blob], name, { type: "text/html" });
      if (matchMedia("(pointer:coarse)").matches && navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({ files: [file], title: document.title });
        return;
      }
    } catch (e) { if (e && e.name === "AbortError") return; }
    download(blob, name);
    toast("File " + name + " terunduh.");
  });

  /* ---------- decorations ---------- */
  (function wave() {
    const W = 100, H = 141.42, m = 4.5, amp = 1.1, len = 6.2;
    const pts = [];
    const edge = (x1, y1, x2, y2, nx, ny) => {
      const L = Math.hypot(x2 - x1, y2 - y1), n = Math.max(2, Math.round(L / len)), steps = n * 12;
      for (let i = 0; i < steps; i++) {
        const t = i / steps, o = Math.sin(t * n * Math.PI * 2) * amp;
        pts.push([x1 + (x2 - x1) * t + nx * o, y1 + (y2 - y1) * t + ny * o]);
      }
    };
    edge(m, m, W - m, m, 0, -1); edge(W - m, m, W - m, H - m, 1, 0);
    edge(W - m, H - m, m, H - m, 0, 1); edge(m, H - m, m, m, -1, 0);
    const wp = $("#wave-path"); if (wp) wp.setAttribute("d", "M" + pts.map(p => p[0].toFixed(2) + " " + p[1].toFixed(2)).join("L") + "Z");
    const burst = [], k = 14;
    for (let i = 0; i < k * 2; i++) { const r = i % 2 ? 30 : 50, a = Math.PI * i / k; burst.push((50 + r * Math.cos(a)).toFixed(1) + "% " + (50 + r * Math.sin(a)).toFixed(1) + "%"); }
    const bu = $("#burst"); if (bu) bu.style.clipPath = "polygon(" + burst.join(",") + ")";
  })();

  /* ---------- boot ---------- */
  (async () => {
    if (embedded && embedded.sid) storeKey = "shared-" + embedded.sid;
    const stored = await dbGet(storeKey);
    state = Object.assign(state, embedded || {}, stored || {});
    state.texts = state.texts || {}; state.photos = state.photos || {}; state.checks = state.checks || {};
    if (!stored && embedded) state.edit = false;
    renderAll();
    layout();
    setEdit(!!state.edit);
    statusEl.textContent = stored ? "Tersimpan" : "";
  })();
})();
