import re
import subprocess
import sys

ADB = r"C:\Users\HP\Android\Sdk\platform-tools\adb.exe"


def dump():
    subprocess.run([ADB, "shell", "uiautomator", "dump", "/sdcard/ui.xml"],
                   capture_output=True)
    out = subprocess.run([ADB, "exec-out", "cat", "/sdcard/ui.xml"],
                         capture_output=True)
    return out.stdout.decode("utf-8", "replace")


def nodes(xml):
    found = []
    for m in re.finditer(r'<node[^>]*>', xml):
        tag = m.group(0)
        text = re.search(r'text="([^"]*)"', tag)
        desc = re.search(r'content-desc="([^"]*)"', tag)
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        label = (text.group(1) if text else "") or (desc.group(1) if desc else "")
        if not label.strip() or not bounds:
            continue
        x1, y1, x2, y2 = map(int, bounds.groups())
        found.append((label.strip(), (x1 + x2) // 2, (y1 + y2) // 2))
    return found


if __name__ == "__main__":
    # Every label in this app is Arabic, and the default Windows console encoding is cp1252,
    # which cannot represent it — printing would die on UnicodeEncodeError before the first row.
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    xml = dump()
    needle = sys.argv[1] if len(sys.argv) > 1 else None
    for label, cx, cy in nodes(xml):
        if needle and needle not in label:
            continue
        print(f"{cx:5d},{cy:5d}  {label}")
