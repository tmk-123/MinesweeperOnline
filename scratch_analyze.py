import zipfile
import xml.etree.ElementTree as ET
import os

def analyze_doc(docx_path, out_file):
    out_file.write(f"==================================================\n")
    out_file.write(f"FILE: {docx_path}\n")
    out_file.write(f"==================================================\n")
    with zipfile.ZipFile(docx_path) as z:
        rels_xml = z.read('word/_rels/document.xml.rels')
        rels_tree = ET.fromstring(rels_xml)
        rel_map = {}
        for r in rels_tree:
            rel_map[r.attrib['Id']] = r.attrib.get('Target', '')

        doc_xml = z.read('word/document.xml')
        doc_tree = ET.fromstring(doc_xml)
        
        body = doc_tree.find('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}body')
        for idx, child in enumerate(body):
            tag = child.tag.split('}')[-1]
            if tag == 'p':
                texts = [node.text for node in child.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t') if node.text]
                p_text = ''.join(texts).strip()
                blips = child.findall('.//{http://schemas.openxmlformats.org/drawingml/2006/main}blip')
                img_targets = [rel_map.get(b.attrib.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}embed'), '') for b in blips]
                if img_targets or p_text:
                    img_str = f" [IMAGES: {', '.join(img_targets)}]" if img_targets else ""
                    out_file.write(f"P{idx}: {p_text}{img_str}\n")
            elif tag == 'tbl':
                out_file.write(f"\nTABLE at body[{idx}]:\n")
                for r_idx, row in enumerate(child.findall('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}tr')):
                    row_texts = []
                    for cell in row.findall('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}tc'):
                        c_texts = [node.text for node in cell.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t') if node.text]
                        row_texts.append(''.join(c_texts).strip())
                    out_file.write(f"  Row {r_idx}: {' | '.join(row_texts)}\n")
                out_file.write("\n")

if __name__ == '__main__':
    with open('docs_analysis.txt', 'w', encoding='utf-8') as f:
        analyze_doc('docs/mo_ta.docx', f)
        analyze_doc('docs/kien_truc.docx', f)
    print("Done writing docs_analysis.txt")
