from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

SOURCE_DIR = Path(r"C:\Users\sai\Documents")
OUTPUT_DIR = Path(r"F:\smartchat-memory-manager\updated_reports")

FILES = [
    "ANSHUL_EXP6_FSJPL.docx",
    "ANSHUL_EXP7_FSJPL.docx",
    "ANSHUL_EXP8_FSJPL.docx",
    "ANSHUL_FSJP_EXP9.docx",
    "ANSHUL_54_FSJP_EXP10.docx",
]

# XML-level substitutions retain all existing paragraph, run, style, table,
# header/footer, image, and section-layout structure from each template.
REPLACEMENTS = {
    "Anshul Talkar": "Sujal Wafekar",
    "ANSHUL TALKAR": "SUJAL WAFEKAR",
    "Anshul": "Sujal",
    "25104011": "25104066",
    "Student ID:": "Moodle ID:",
    ">Student<": ">Moodle<",
    "Roll No. : 54": "Roll No. : 63",
    "Roll No.: 54": "Roll No.: 63",
    "ROLL NO.: 54": "ROLL NO.: 63",
    'const rno="21"': 'const rno="63"',
    'const rno=&quot;21&quot;': 'const rno=&quot;63&quot;',
    '>&quot;21&quot;<': '>&quot;63&quot;<',
    ">54<": ">63<",
}


def transform(source: Path, destination: Path) -> None:
    with ZipFile(source, "r") as input_zip, ZipFile(destination, "w", ZIP_DEFLATED) as output_zip:
        for item in input_zip.infolist():
            data = input_zip.read(item.filename)
            if item.filename.startswith("word/") and item.filename.endswith(".xml"):
                text = data.decode("utf-8")
                for old, new in REPLACEMENTS.items():
                    text = text.replace(old, new)
                data = text.encode("utf-8")
            output_zip.writestr(item, data)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for name in FILES:
        source = SOURCE_DIR / name
        output_name = name.replace("ANSHUL", "SUJAL").replace("_54_FSJP_", "_63_FSJP_")
        destination = OUTPUT_DIR / output_name
        transform(source, destination)
        print(destination)


if __name__ == "__main__":
    main()
