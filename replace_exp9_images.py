from io import BytesIO
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"F:\smartchat-memory-manager")
DOCX = ROOT / "updated_reports" / "SUJAL_FSJP_EXP9.docx"
FONT = Path(r"C:\Windows\Fonts\consola.ttf")
BOLD_FONT = Path(r"C:\Windows\Fonts\consolab.ttf")

SOURCES = {
    "image2.jpeg": ("Application entry point", "src/main/java/com/smartchat/memorymanager/SmartChatApplication.java"),
    "image3.jpeg": ("JWT authentication API", "src/main/java/com/smartchat/memorymanager/controller/AuthController.java"),
    "image4.jpeg": ("Memory entity", "src/main/java/com/smartchat/memorymanager/model/Memory.java"),
    "image5.jpeg": ("Memory repository", "src/main/java/com/smartchat/memorymanager/repository/MemoryRepository.java"),
    "image6.jpeg": ("Memory service", "src/main/java/com/smartchat/memorymanager/service/MemoryService.java"),
    "image7.jpeg": ("Memory-aware chat service", "src/main/java/com/smartchat/memorymanager/service/ChatService.java"),
    "image8.jpeg": ("Chat REST controller", "src/main/java/com/smartchat/memorymanager/controller/ChatController.java"),
    "image9.jpeg": ("Chat workspace client", "src/main/resources/static/chat/script.js"),
}


def font(size: int, bold: bool = False):
    return ImageFont.truetype(str(BOLD_FONT if bold else FONT), size)


def code_image(size: tuple[int, int], title: str, relative_path: str) -> bytes:
    image = Image.new("RGB", size, "#101612")
    draw = ImageDraw.Draw(image)
    width, height = size
    draw.rectangle((0, 0, width, 72), fill="#183126")
    draw.text((35, 19), "SmartChat Memory Manager", font=font(30, True), fill="#DDF5E5")
    draw.text((width - 420, 23), title, font=font(20), fill="#9BD1AD")
    draw.rectangle((26, 96, width - 26, height - 26), fill="#141D17", outline="#385543", width=2)
    draw.text((56, 118), relative_path.replace("/", "  /  "), font=font(20, True), fill="#A7D9B6")
    draw.line((56, 153, width - 56, 153), fill="#385543", width=2)
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    lines = text.splitlines()
    y = 181
    line_height = 25
    max_lines = (height - y - 42) // line_height
    for number, line in enumerate(lines[:max_lines], start=1):
        draw.text((55, y), f"{number:>2}", font=font(17), fill="#5F8B6B")
        # Long source lines are clipped deliberately, as in an IDE screenshot.
        draw.text((105, y), line[:120], font=font(17), fill="#E2E8E3")
        y += line_height
    out = BytesIO()
    image.save(out, format="JPEG", quality=92, optimize=True)
    return out.getvalue()


def banner(size: tuple[int, int]) -> bytes:
    image = Image.new("RGB", size, "#183126")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, size[0], size[1]), outline="#6FB585", width=4)
    draw.text((34, 28), "SMARTCHAT MEMORY MANAGER", font=font(33, True), fill="#E7F7EC")
    draw.text((34, 76), "Spring Boot REST API and memory-aware chat workspace", font=font(19), fill="#B5DEC0")
    out = BytesIO()
    image.save(out, format="JPEG", quality=92, optimize=True)
    return out.getvalue()


def main() -> None:
    temp = DOCX.with_suffix(".replacing.docx")
    with ZipFile(DOCX, "r") as source, ZipFile(temp, "w", ZIP_DEFLATED) as destination:
        for item in source.infolist():
            data = source.read(item.filename)
            name = Path(item.filename).name
            if item.filename == "word/media/image1.jpeg":
                with Image.open(BytesIO(data)) as original:
                    data = banner(original.size)
            elif name in SOURCES:
                title, path = SOURCES[name]
                with Image.open(BytesIO(data)) as original:
                    data = code_image(original.size, title, path)
            destination.writestr(item, data)
    temp.replace(DOCX)


if __name__ == "__main__":
    main()
