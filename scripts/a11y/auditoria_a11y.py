#!/usr/bin/env python3
"""Auditoría de accesibilidad de la pantalla visible de TARE·IA (vía adb + uiautomator).

Reglas (guías de accesibilidad de Android):
  R1  Todo elemento pulsable mide al menos 48 x 48 dp.
  R2  Todo elemento pulsable tiene un nombre accesible (texto o contentDescription,
      propio o de un hijo), que es lo que lee TalkBack.

Los elementos recortados por el borde de la pantalla se omiten: deben auditarse
después de desplazarlos a la vista. Un elemento tapado por la barra inferior
puede aparecer como falso positivo de R1 (ver docs/AUDITORIA_UI.md).

Uso:
  python scripts/a11y/auditoria_a11y.py [--nombre "Inicio"] [--adb RUTA_ADB] [--paquete com.tareia.app]
Código de salida: 0 sin hallazgos, 1 con hallazgos.
"""
import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def adb(args, ruta_adb):
    return subprocess.run([ruta_adb, *args], capture_output=True, check=True).stdout.decode("utf-8", "replace")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--nombre", default="pantalla actual", help="Etiqueta para el informe")
    p.add_argument("--adb", default="adb", help="Ruta del ejecutable adb")
    p.add_argument("--paquete", default="com.tareia.app")
    a = p.parse_args()

    dpi = int(re.search(r"(\d+)\s*$", adb(["shell", "wm", "density"], a.adb).strip().splitlines()[-1]).group(1))
    adb(["shell", "uiautomator", "dump", "/sdcard/tareia_a11y.xml"], a.adb)
    raw = adb(["exec-out", "cat", "/sdcard/tareia_a11y.xml"], a.adb)
    raw = raw[raw.find("<?xml"): raw.rfind("</hierarchy>") + len("</hierarchy>")]
    root = ET.fromstring(raw)

    minimo = 48 * dpi / 160 - 2  # px, con tolerancia de redondeo
    caja = lambda n: list(map(int, re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))))
    alto_pantalla = max(caja(n)[3] for n in root.iter("node"))

    def nombre(n):
        for m in n.iter():
            t = (m.get("text") or "").strip() or (m.get("content-desc") or "").strip()
            if t:
                return t
        return ""

    hallazgos = []
    for n in root.iter("node"):
        if n.get("package") != a.paquete or n.get("clickable") != "true":
            continue
        x0, y0, x1, y1 = caja(n)
        w, h = x1 - x0, y1 - y0
        if w == 0 or h == 0 or y0 <= 0 or y1 >= alto_pantalla:
            continue
        etiqueta = nombre(n)
        if w < minimo or h < minimo:
            hallazgos.append(f"R1 área {w}x{h}px (mín. {minimo + 2:.0f}px = 48dp) en «{etiqueta or n.get('class')}»")
        if not etiqueta:
            hallazgos.append(f"R2 sin nombre accesible: {n.get('class')} {n.get('bounds')}")

    print(f"[{a.nombre}] densidad {dpi} dpi · {'OK' if not hallazgos else f'{len(hallazgos)} hallazgo(s)'}")
    for h in hallazgos:
        print("   ", h)
    sys.exit(1 if hallazgos else 0)


if __name__ == "__main__":
    main()
