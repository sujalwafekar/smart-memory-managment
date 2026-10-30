Add-Type -AssemblyName System.Drawing

$canvas = [System.Drawing.Bitmap]::new(1600, 950)
$graphics = [System.Drawing.Graphics]::FromImage($canvas)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(246, 248, 243))

$green = [System.Drawing.Color]::FromArgb(27, 107, 75)
$dark = [System.Drawing.Color]::FromArgb(36, 55, 45)
$muted = [System.Drawing.Color]::FromArgb(92, 115, 101)
$border = [System.Drawing.Pen]::new($green, 4)
$connector = [System.Drawing.Pen]::new($green, 4)
$titleFont = [System.Drawing.Font]::new('Arial', 34, [System.Drawing.FontStyle]::Bold)
$subFont = [System.Drawing.Font]::new('Arial', 17)
$headFont = [System.Drawing.Font]::new('Arial', 23, [System.Drawing.FontStyle]::Bold)
$bodyFont = [System.Drawing.Font]::new('Consolas', 17)
$labelFont = [System.Drawing.Font]::new('Arial', 18, [System.Drawing.FontStyle]::Bold)
$brushDark = [System.Drawing.SolidBrush]::new($dark)
$brushGreen = [System.Drawing.SolidBrush]::new($green)
$brushMuted = [System.Drawing.SolidBrush]::new($muted)
$brushWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)

$graphics.DrawString('MemoryChat - Entity Relationship Diagram', $titleFont, $brushDark, 100, 72)
$graphics.DrawString('MySQL schema: one user owns zero or many saved memories', $subFont, $brushMuted, 102, 125)

function Draw-Entity($x, $y, $width, $height, $name, $fields) {
    $graphics.FillRectangle($brushWhite, $x, $y, $width, $height)
    $graphics.DrawRectangle($border, $x, $y, $width, $height)
    $graphics.FillRectangle($brushGreen, $x, $y, $width, 62)
    $graphics.DrawString($name, $headFont, $brushWhite, $x + 24, $y + 17)
    $fieldY = $y + 94
    foreach ($field in $fields) {
        $graphics.DrawString($field, $bodyFont, $brushDark, $x + 25, $fieldY)
        $fieldY += 42
    }
}

Draw-Entity 130 255 470 310 'APP_USERS' @('PK  id : BIGINT, AUTO_INCREMENT', 'username : VARCHAR(60), UNIQUE', 'password : VARCHAR(255)', 'created_at : DATETIME')
Draw-Entity 970 195 500 430 'MEMORIES' @('PK  id : BIGINT, AUTO_INCREMENT', 'type : VARCHAR(20)', 'content : TEXT', 'importance : DOUBLE', 'created_at : DATETIME', 'FK  owner_id : BIGINT')

$graphics.DrawLine($connector, 600, 410, 970, 410)
$graphics.DrawLine($connector, 615, 390, 615, 430)
$graphics.DrawLine($connector, 635, 390, 635, 430)
$graphics.DrawLine($connector, 930, 378, 970, 410)
$graphics.DrawLine($connector, 930, 442, 970, 410)
$graphics.DrawLine($connector, 930, 410, 970, 410)
$graphics.DrawString('owns', $labelFont, $brushGreen, 740, 365)
$graphics.DrawString('1', $labelFont, $brushGreen, 630, 455)
$graphics.DrawString('0..*', $labelFont, $brushGreen, 900, 455)
$graphics.DrawString('Relationship: APP_USERS.id -> MEMORIES.owner_id', $subFont, $brushDark, 500, 725)
$graphics.DrawString('The foreign key ensures that each saved memory belongs to one authenticated user.', $subFont, $brushMuted, 390, 765)

$output = Join-Path $PSScriptRoot '..\diagrams\MemoryChat_ER_Diagram.png'
$canvas.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$canvas.Dispose()
Write-Output $output
