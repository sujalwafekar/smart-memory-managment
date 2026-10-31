Add-Type -AssemblyName System.Drawing

$canvas = [System.Drawing.Bitmap]::new(1800, 1120)
$graphics = [System.Drawing.Graphics]::FromImage($canvas)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
$graphics.Clear([System.Drawing.Color]::White)

$black = [System.Drawing.Color]::Black
$line = [System.Drawing.Pen]::new($black, 3)
$thin = [System.Drawing.Pen]::new($black, 2)
$brush = [System.Drawing.SolidBrush]::new($black)
$titleFont = [System.Drawing.Font]::new('Times New Roman', 32, [System.Drawing.FontStyle]::Bold)
$noteFont = [System.Drawing.Font]::new('Times New Roman', 17)
$entityFont = [System.Drawing.Font]::new('Times New Roman', 23, [System.Drawing.FontStyle]::Bold)
$attributeFont = [System.Drawing.Font]::new('Times New Roman', 17)
$relationFont = [System.Drawing.Font]::new('Times New Roman', 20, [System.Drawing.FontStyle]::Bold)
$cardinalityFont = [System.Drawing.Font]::new('Times New Roman', 18, [System.Drawing.FontStyle]::Bold)

function Center-Text($text, $font, $x, $y, $width, $height) {
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $graphics.DrawString($text, $font, $brush, [System.Drawing.RectangleF]::new($x, $y, $width, $height), $format)
    $format.Dispose()
}

function Draw-Entity($name, $x, $y, $width, $height) {
    $graphics.DrawRectangle($line, $x, $y, $width, $height)
    Center-Text $name $entityFont $x $y $width $height
}

function Draw-Attribute($name, $x, $y, $width, $height, $isKey) {
    $graphics.DrawEllipse($thin, $x, $y, $width, $height)
    Center-Text $name $attributeFont $x $y $width $height
    if ($isKey) {
        $size = $graphics.MeasureString($name, $attributeFont)
        $startX = $x + ($width - $size.Width) / 2
        $baseline = $y + $height / 2 + $size.Height / 2 + 2
        $graphics.DrawLine($thin, $startX, $baseline, $startX + $size.Width, $baseline)
    }
}

function Draw-Relationship($name, $centerX, $centerY, $size) {
    $half = $size / 2
    $points = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new($centerX, $centerY - $half),
        [System.Drawing.PointF]::new($centerX + $half, $centerY),
        [System.Drawing.PointF]::new($centerX, $centerY + $half),
        [System.Drawing.PointF]::new($centerX - $half, $centerY)
    )
    $graphics.DrawPolygon($line, $points)
    Center-Text $name $relationFont ($centerX - $half) ($centerY - 24) $size 48
}

$graphics.DrawString('MemoryChat - Entity Relationship Diagram', $titleFont, $brush, 500, 46)
$graphics.DrawString('Chen notation: entities, attributes, relationship, and cardinality', $noteFont, $brush, 545, 95)

# Connectors are drawn first so symbols stay clear and readable.
$graphics.DrawLine($line, 630, 520, 790, 520)
$graphics.DrawLine($line, 1010, 520, 1170, 520)

# APP_USERS attributes.
$graphics.DrawLine($thin, 420, 410, 390, 245)
$graphics.DrawLine($thin, 310, 460, 160, 385)
$graphics.DrawLine($thin, 300, 570, 155, 650)
$graphics.DrawLine($thin, 430, 630, 395, 810)

# MEMORIES attributes.
$graphics.DrawLine($thin, 1360, 410, 1390, 245)
$graphics.DrawLine($thin, 1470, 460, 1630, 385)
$graphics.DrawLine($thin, 1480, 570, 1640, 570)
$graphics.DrawLine($thin, 1365, 630, 1400, 805)
$graphics.DrawLine($thin, 1260, 630, 1145, 755)
$graphics.DrawLine($thin, 1470, 600, 1610, 710)

Draw-Entity 'APP_USERS' 310 410 320 220
Draw-Entity 'MEMORIES' 1170 410 320 220
Draw-Relationship 'OWNS' 900 520 220

Draw-Attribute 'User ID (PK)' 285 150 210 70 $true
Draw-Attribute 'Username' 35 345 200 70 $false
Draw-Attribute 'Password' 30 615 205 70 $false
Draw-Attribute 'Created At' 290 805 210 70 $false

Draw-Attribute 'Memory ID (PK)' 1285 150 220 70 $true
Draw-Attribute 'Type' 1555 345 150 70 $false
Draw-Attribute 'Content' 1560 535 190 70 $false
Draw-Attribute 'Importance' 1310 800 205 70 $false
Draw-Attribute 'Created At' 1035 730 205 70 $false
Draw-Attribute 'Owner ID (FK)' 1540 690 220 70 $false

Center-Text '1' $cardinalityFont 685 488 45 40
Center-Text 'N' $cardinalityFont 1070 488 45 40
$graphics.DrawString('One user can own many saved memories.', $noteFont, $brush, 635, 1015)
$graphics.DrawString('Each memory record belongs to exactly one authenticated user.', $noteFont, $brush, 540, 1045)

$output = Join-Path $PSScriptRoot '..\diagrams\MemoryChat_Chen_ER_Diagram.png'
$canvas.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)

$line.Dispose()
$thin.Dispose()
$brush.Dispose()
$titleFont.Dispose()
$noteFont.Dispose()
$entityFont.Dispose()
$attributeFont.Dispose()
$relationFont.Dispose()
$cardinalityFont.Dispose()
$graphics.Dispose()
$canvas.Dispose()

Write-Output $output
