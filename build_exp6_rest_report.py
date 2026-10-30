from io import BytesIO
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"F:\smartchat-memory-manager")
TEMPLATE = Path(r"C:\Users\sai\Documents\exp6.docx")
OUTPUT = ROOT / "updated_reports" / "SUJAL_EXP6_RESTFUL_SPRING_BOOT.docx"
ASSET_DIR = ROOT / "rest_exp6_assets"
FONT = Path(r"C:\Windows\Fonts\consola.ttf")
BOLD_FONT = Path(r"C:\Windows\Fonts\consolab.ttf")

SOURCE_FILES = [
    ("Memory REST Controller", "src/main/java/com/smartchat/memorymanager/controller/MemoryController.java"),
    ("Chat REST Controller", "src/main/java/com/smartchat/memorymanager/controller/ChatController.java"),
    ("Memory Service", "src/main/java/com/smartchat/memorymanager/service/MemoryService.java"),
    ("Memory Repository", "src/main/java/com/smartchat/memorymanager/repository/MemoryRepository.java"),
]


def set_cell_shading(run, fill: str) -> None:
    rpr = run._element.get_or_add_rPr()
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), fill)
    rpr.append(shading)


def set_font(run, name: str, size: int, color: str = "000000") -> None:
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:ascii"), name)
    run._element.rPr.rFonts.set(qn("w:hAnsi"), name)
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor.from_string(color)


def add_code(doc: Document, filename: str, content: str) -> None:
    label = doc.add_paragraph()
    label.paragraph_format.space_before = Pt(5)
    label.paragraph_format.space_after = Pt(2)
    run = label.add_run(filename)
    run.bold = True
    set_font(run, "Arial", 10)
    for line in content.splitlines():
        paragraph = doc.add_paragraph()
        paragraph.paragraph_format.space_after = Pt(0)
        paragraph.paragraph_format.line_spacing = 1
        run = paragraph.add_run(line or " ")
        set_font(run, "Consolas", 7, "F2F2F2")
        set_cell_shading(run, "18231B")


def code_screenshot(title: str, relative_path: str, out_path: Path) -> None:
    width, height = 1800, 1030
    image = Image.new("RGB", (width, height), "#111A13")
    draw = ImageDraw.Draw(image)
    regular = ImageFont.truetype(str(FONT), 22)
    strong = ImageFont.truetype(str(BOLD_FONT), 29)
    draw.rectangle((0, 0, width, 78), fill="#1C3A29")
    draw.text((36, 22), "SmartChat Memory Manager", font=strong, fill="#EBF8EE")
    draw.text((width - 430, 27), title, font=regular, fill="#B7E0C1")
    draw.rectangle((30, 105, width - 30, height - 32), fill="#141D17", outline="#40604A", width=2)
    draw.text((62, 130), relative_path.replace("/", "  /  "), font=regular, fill="#A9DDB5")
    draw.line((62, 167, width - 62, 167), fill="#3B5B44", width=2)
    y = 197
    for index, line in enumerate((ROOT / relative_path).read_text(encoding="utf-8").splitlines()[:30], start=1):
        draw.text((65, y), f"{index:>2}", font=regular, fill="#6D9D77")
        draw.text((122, y), line[:120], font=regular, fill="#F0F3F0")
        y += 27
    image.save(out_path, format="JPEG", quality=91)


def clear_document_body(doc: Document) -> None:
    body = doc._element.body
    for child in list(body):
        if child.tag != qn("w:sectPr"):
            body.remove(child)


def add_heading(doc: Document, text: str, level: int = 1) -> None:
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.space_before = Pt(8)
    paragraph.paragraph_format.space_after = Pt(4)
    run = paragraph.add_run(text)
    run.bold = True
    set_font(run, "Arial", 13 if level == 1 else 11)


def main() -> None:
    ASSET_DIR.mkdir(exist_ok=True)
    for index, (title, path) in enumerate(SOURCE_FILES, start=1):
        code_screenshot(title, path, ASSET_DIR / f"screen_{index}.jpg")

    doc = Document(TEMPLATE)
    clear_document_body(doc)
    section = doc.sections[0]
    section.top_margin = Inches(0.65)
    section.bottom_margin = Inches(0.65)

    info = [
        "Academic Year: 2026-27     Name of Student: Sujal Wafekar",
        "Semester: III     Moodle ID: 25104066",
        "Class / Branch / Div: SE IT C     Roll No.: 63",
        "Subject: FSJPL     Name of Instructor: Ms. Rucha Kulkarni",
        "Experiment No.: 6",
    ]
    for line in info:
        paragraph = doc.add_paragraph()
        paragraph.paragraph_format.space_after = Pt(1)
        run = paragraph.add_run(line)
        set_font(run, "Arial", 10)

    add_heading(doc, "Aim")
    doc.add_paragraph("To develop RESTful web services for the SmartChat Memory Manager using Spring Boot and expose endpoints for authenticated memory and chat operations.")
    add_heading(doc, "Code")
    excerpts = [
        ("MemoryController.java", ROOT / SOURCE_FILES[0][1]),
        ("ChatController.java", ROOT / SOURCE_FILES[1][1]),
        ("MemoryRepository.java", ROOT / SOURCE_FILES[3][1]),
    ]
    for index, (name, path) in enumerate(excerpts, start=1):
        if index > 1:
            doc.add_page_break()
        add_heading(doc, name, 2)
        add_code(doc, name, path.read_text(encoding="utf-8"))

    doc.add_page_break()
    add_heading(doc, "MemoryService.java", 2)
    service_lines = (ROOT / SOURCE_FILES[2][1]).read_text(encoding="utf-8").splitlines()
    add_code(doc, "MemoryService.java", "\n".join(service_lines[:80]))

    doc.add_page_break()
    add_heading(doc, "Output")
    doc.add_paragraph("The following screenshots show the REST API components implemented for the SmartChat Memory Manager project.")
    for index, (title, _path) in enumerate(SOURCE_FILES, start=1):
        add_heading(doc, title, 2)
        picture = doc.add_picture(str(ASSET_DIR / f"screen_{index}.jpg"), width=Inches(6.1))
        doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
        if index < len(SOURCE_FILES):
            doc.add_page_break()
    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    main()
