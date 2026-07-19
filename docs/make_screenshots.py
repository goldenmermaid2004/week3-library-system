"""Capture console demo screenshots as PNG images for the Week 3 documentation."""
from __future__ import annotations

import pathlib
import subprocess
import sys
import textwrap

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
BIN = ROOT / "bin"


def find_font(size: int = 15):
    candidates = [
        r"C:\Windows\Fonts\consola.ttf",
        r"C:\Windows\Fonts\lucon.ttf",
        r"C:\Windows\Fonts\cour.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ]
    for path in candidates:
        if pathlib.Path(path).exists():
            return ImageFont.truetype(path, size=size)
    return ImageFont.load_default()


def render_console(lines: list[str], outfile: pathlib.Path, title: str = "") -> None:
    font = find_font(15)
    title_font = find_font(13)
    pad_x, pad_y = 28, 24
    line_h = 22
    max_width = max((len(line) for line in lines), default=40)
    width = min(980, max(720, pad_x * 2 + max_width * 9))
    height = pad_y * 2 + line_h * (len(lines) + (2 if title else 0)) + 20

    img = Image.new("RGB", (width, height), (18, 22, 28))
    draw = ImageDraw.Draw(img)

    # Title bar
    draw.rectangle([0, 0, width, 34], fill=(32, 38, 48))
    draw.ellipse([14, 12, 24, 22], fill=(255, 95, 86))
    draw.ellipse([30, 12, 40, 22], fill=(255, 189, 46))
    draw.ellipse([46, 12, 56, 22], fill=(39, 201, 63))
    if title:
        draw.text((70, 9), title, fill=(180, 190, 205), font=title_font)

    y = 46
    for line in lines:
        # Soft highlight for section headers
        stripped = line.strip()
        color = (210, 218, 228)
        if stripped.startswith("===") or stripped.startswith("---"):
            color = (110, 190, 255)
        elif stripped.startswith("Loaded") or "successfully" in stripped.lower():
            color = (120, 220, 160)
        elif "OVERDUE" in stripped or "Error" in stripped or "not found" in stripped.lower():
            color = (255, 150, 140)
        elif stripped.startswith("Enter your choice") or stripped.endswith(":"):
            color = (200, 180, 120)
        draw.text((pad_x, y), line, fill=color, font=font)
        y += line_h

    img.save(outfile)
    print("Wrote", outfile)


def run_java(input_text: str) -> str:
    if not (BIN / "library" / "Main.class").exists():
        compile_cmd = ["javac", "-d", "bin", "src/main/java/library/Book.java",
                       "src/main/java/library/Member.java",
                       "src/main/java/library/FileHandler.java",
                       "src/main/java/library/Library.java",
                       "src/main/java/library/Main.java"]
        subprocess.run(compile_cmd, cwd=ROOT, check=True)

    proc = subprocess.run(
        ["java", "-cp", "bin", "library.Main"],
        input=input_text,
        text=True,
        capture_output=True,
        cwd=ROOT,
        timeout=30,
    )
    return (proc.stdout or "") + (proc.stderr or "")


def trim_lines(text: str, max_lines: int = 28) -> list[str]:
    lines = [ln.rstrip() for ln in text.replace("\r\n", "\n").split("\n")]
    # Drop trailing empties
    while lines and not lines[-1]:
        lines.pop()
    if len(lines) > max_lines:
        lines = lines[:max_lines]
    # Wrap very long lines for the image
    wrapped: list[str] = []
    for line in lines:
        if len(line) <= 100:
            wrapped.append(line)
        else:
            wrapped.extend(textwrap.wrap(line, width=100) or [line])
    return wrapped


def main() -> int:
    DOCS.mkdir(exist_ok=True)

    demos = {
        "screenshot-menu.png": (
            "Console menu on startup",
            "14\n",
            lambda out: out.split("Enter your choice:")[0] + "Enter your choice:",
        ),
        "screenshot-books.png": (
            "View all books",
            "2\n14\n",
            lambda out: "=== ALL BOOKS ===" + out.split("=== ALL BOOKS ===", 1)[-1].split("=== LIBRARY MANAGEMENT SYSTEM ===")[0],
        ),
        "screenshot-search.png": (
            "Search books for 'Java'",
            "3\nJava\n14\n",
            lambda out: "=== SEARCH RESULTS ===" + out.split("=== SEARCH RESULTS ===", 1)[-1].split("=== LIBRARY MANAGEMENT SYSTEM ===")[0],
        ),
        "screenshot-stats.png": (
            "Library statistics",
            "12\n14\n",
            lambda out: "=== LIBRARY STATISTICS ===" + out.split("=== LIBRARY STATISTICS ===", 1)[-1].split("=== LIBRARY MANAGEMENT SYSTEM ===")[0],
        ),
        "screenshot-overdue.png": (
            "Overdue books and fines",
            "11\n14\n",
            lambda out: "=== OVERDUE BOOKS ===" + out.split("=== OVERDUE BOOKS ===", 1)[-1].split("=== LIBRARY MANAGEMENT SYSTEM ===")[0],
        ),
    }

    for filename, (title, stdin, extractor) in demos.items():
        raw = run_java(stdin)
        try:
            section = extractor(raw).strip("\n")
        except Exception:
            section = raw
        # Keep a short banner so screenshots look like a real session
        header = [
            "========================================",
            "   LIBRARY MANAGEMENT SYSTEM",
            "========================================",
            "",
        ]
        body = trim_lines(section, max_lines=22)
        render_console(header + body, DOCS / filename, title=title)

    # Validation / error feedback screenshot (static demo text for clarity)
    validation_lines = [
        "--- Add New Book ---",
        "ISBN: ",
        "Input cannot be empty. Please try again.",
        "ISBN: 9780134685991",
        "A book with that ISBN already exists.",
        "",
        "--- Register Member ---",
        "Member ID (e.g. MEM001): MEM001",
        "A member with that ID already exists.",
        "",
        "--- Borrow Book ---",
        "ISBN: 9999999999999",
        "Member ID: MEM001",
        "Book not found!",
        "",
        "Please enter a number between 1 and 14: abc",
        "Invalid input. Enter a number: ",
    ]
    render_console(validation_lines, DOCS / "screenshot-validation.png",
                   title="Input validation and error feedback")

    print("All screenshots generated.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
