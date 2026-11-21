#!/usr/bin/env python3
"""Rakit setiap desain di src/designs/<id>/ menjadi satu file HTML mandiri di desain/<id>.html.

Jalankan: python3 scrapbook/build.py
Setiap desain berisi meta.json, style.css, dan pages.html. Mesin bersama ada di src/engine.css dan src/engine.js.
"""
import json, pathlib, html

ROOT = pathlib.Path(__file__).resolve().parent
SRC = ROOT / "src"
OUT = ROOT / "desain"
OUT.mkdir(exist_ok=True)

shell = (SRC / "shell.html").read_text()
engine_css = (SRC / "engine.css").read_text().rstrip()
engine_js = (SRC / "engine.js").read_text().rstrip()

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
        "{{DESIGN_CSS}}": (d / "style.css").read_text().rstrip(),
        "{{PAGES}}": (d / "pages.html").read_text().rstrip(),
        "{{ENGINE_JS}}": engine_js,
    }.items():
        out = out.replace(k, v)
    (OUT / f"{d.name}.html").write_text(out)
    print("built", f"desain/{d.name}.html")
