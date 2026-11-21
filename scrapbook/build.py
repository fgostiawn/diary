#!/usr/bin/env python3
"""Rakit setiap desain di src/designs/<id>/ menjadi satu file HTML mandiri di desain/<id>.html.

Jalankan: python3 scrapbook/build.py
Setiap desain berisi meta.json, style.css, dan pages.html. Mesin bersama ada di src/engine.css dan src/engine.js.
"""
import json, pathlib, html, base64, re, mimetypes

ROOT = pathlib.Path(__file__).resolve().parent
SRC = ROOT / "src"
OUT = ROOT / "desain"
OUT.mkdir(exist_ok=True)

shell = (SRC / "shell.html").read_text()
engine_css = (SRC / "engine.css").read_text().rstrip()
engine_js = (SRC / "engine.js").read_text().rstrip()

def inline_assets(css, d):
    """url("asset:nama.jpg") -> data URI, supaya file tetap mandiri (bisa dibagikan sebagai satu file)."""
    def rep(m):
        f = d / "assets" / m.group(1)
        mime = mimetypes.guess_type(f.name)[0] or "application/octet-stream"
        return 'url("data:%s;base64,%s")' % (mime, base64.b64encode(f.read_bytes()).decode())
    return re.sub(r'url\("asset:([^"]+)"\)', rep, css)


for d in sorted((SRC / "designs").iterdir()):
    if not d.is_dir():
        continue
    meta = json.loads((d / "meta.json").read_text())
    fonts = "".join("&family=" + f for f in meta.get("fonts", []))
    mode = meta.get("mode", "land")
    out = shell
    for k, v in {
        "{{ID}}": d.name,
        "{{MODE}}": mode,
        "{{PAGESIZE}}": "landscape" if mode == "land" else "portrait",
        "{{TITLE}}": html.escape(meta["title"]),
        "{{SUB}}": html.escape(meta.get("sub", "")),
        "{{DESC}}": html.escape(meta.get("desc", "")),
        "{{FONTS}}": fonts,
        "{{ENGINE_CSS}}": engine_css,
        "{{DESIGN_CSS}}": inline_assets((d / "style.css").read_text().rstrip(), d),
        "{{PAGES}}": (d / "pages.html").read_text().rstrip(),
        "{{ENGINE_JS}}": engine_js,
    }.items():
        out = out.replace(k, v)
    (OUT / f"{d.name}.html").write_text(out)
    print("built", f"desain/{d.name}.html")
