"""Genera los íconos PNG de la PWA sin dependencias externas al repo.

Uso: python pwa/tools/make_icons.py
Requiere Pillow. El diseño replica el ícono de la app Android: fondo #111,
carpeta de línea clara y un check blanco.
"""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ICONS = ROOT / "icons"
BG = (17, 17, 17, 255)
FOLDER = (242, 242, 242, 255)
CHECK = (255, 255, 255, 255)


def draw_icon(size: int, corner_radius: float, supersample: int = 4) -> Image.Image:
    big = size * supersample
    image = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    radius = int(corner_radius * big)
    draw.rounded_rectangle((0, 0, big - 1, big - 1), radius=radius, fill=BG)

    s = big
    left = 0.21 * s
    right = 0.79 * s
    top = 0.30 * s
    bottom = 0.72 * s
    tab_right = left + 0.35 * s
    step = 0.07 * s

    folder = [
        (left, top + step),
        (left + step, top),
        (tab_right, top),
        (tab_right + step, top + step),
        (right - step, top + step),
        (right, top + 2 * step),
        (right, bottom - step),
        (right - step, bottom),
        (left + step, bottom),
        (left, bottom - step),
        (left, top + step),
    ]
    draw.line(folder, fill=FOLDER, width=int(0.042 * s), joint="curve")

    check = [
        (0.40 * s, 0.53 * s),
        (0.48 * s, 0.61 * s),
        (0.63 * s, 0.44 * s),
    ]
    check_width = int(0.055 * s)
    draw.line(check, fill=CHECK, width=check_width, joint="curve")
    cap = check_width // 2
    for x, y in (check[0], check[-1]):
        draw.ellipse((x - cap, y - cap, x + cap, y + cap), fill=CHECK)

    return image.resize((size, size), Image.LANCZOS)


def save(image: Image.Image, name: str) -> None:
    ICONS.mkdir(parents=True, exist_ok=True)
    image.save(ICONS / name, format="PNG", optimize=True)
    print(f"icons/{name}")


def main() -> None:
    save(draw_icon(192, 0.22), "icon-192.png")
    save(draw_icon(512, 0.22), "icon-512.png")
    save(draw_icon(512, 0.0), "icon-maskable-512.png")
    save(draw_icon(180, 0.0), "apple-touch-icon.png")
    save(draw_icon(32, 0.24, supersample=8), "favicon-32.png")


if __name__ == "__main__":
    main()
