"""Cập nhật số dòng trong tài liệu luồng theo dấu mốc `Tìm` ở mã nguồn.

Chạy từ thư mục dự án: python tools/update_flow_references.py
"""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
REFERENCE = re.compile(
    r"\[([^\]]+ — dòng )(\d+)\]\(([^)]+)#L(\d+)\)\. Tìm `([^`]+)`"
)
TABLE_REFERENCE = re.compile(
    r"\[([^\]\n]+\.java:)(\d+)\]\(([^)\n]+)#L(\d+)\)( \| `)([^`\n]+)(` \|)"
)


def main():
    updates = []
    checked = 0
    changed = 0
    for document in sorted((ROOT / "docs").rglob("*.md")):
        original = document.read_bytes()
        content = original.decode("utf-8")

        def replace(match):
            nonlocal checked, changed
            source = (document.parent / match[3]).resolve()
            lines = source.read_text(encoding="utf-8-sig").splitlines()
            candidates = [
                number for number, line in enumerate(lines, 1)
                if match[5] in line and not line.strip().startswith(("//", "*"))
            ]
            if not candidates:
                raise ValueError(f"Không tìm thấy dấu mốc trong {source}: {match[5]}")
            # Dấu mốc lặp (ví dụ catch) lấy vị trí gần dòng cũ nhất.
            number = min(candidates, key=lambda value: abs(value - int(match[2])))
            checked += 1
            if str(number) != match[2] or str(number) != match[4]:
                changed += 1
            return f"[{match[1]}{number}]({match[3]}#L{number}). Tìm `{match[5]}`"

        updated = REFERENCE.sub(replace, content).encode("utf-8")
        def replace_table(match):
            nonlocal checked, changed
            source = (document.parent / match[3]).resolve()
            lines = source.read_text(encoding="utf-8-sig").splitlines()
            candidates = [
                number for number, line in enumerate(lines, 1)
                if match[6] in line and not line.strip().startswith(("//", "*"))
            ]
            if not candidates:
                raise ValueError(f"Không tìm thấy dấu mốc trong {source}: {match[6]}")
            number = min(candidates, key=lambda value: abs(value - int(match[2])))
            checked += 1
            if str(number) != match[2] or str(number) != match[4]:
                changed += 1
            return f"[{match[1]}{number}]({match[3]}#L{number}){match[5]}{match[6]}{match[7]}"

        updated = TABLE_REFERENCE.sub(replace_table, updated.decode("utf-8")).encode("utf-8")
        if updated != original:
            updates.append((document, updated))

    # Chỉ ghi sau khi mọi dấu mốc đều được tìm thấy để tránh cập nhật dở dang.
    for document, updated in updates:
        document.write_bytes(updated)
    print(f"Đã kiểm tra {checked} tham chiếu, sửa {changed} vị trí trong {len(updates)} tài liệu.")


if __name__ == "__main__":
    main()
