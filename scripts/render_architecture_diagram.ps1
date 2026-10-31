Add-Type -AssemblyName System.Drawing

$canvas = [System.Drawing.Bitmap]::new(1900, 1120)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
$g.Clear([System.Drawing.Color]::FromArgb(20, 22, 26))

$white = [System.Drawing.Color]::FromArgb(241, 244, 248)
$muted = [System.Drawing.Color]::FromArgb(169, 180, 191)
$lineColor = [System.Drawing.Color]::FromArgb(205, 214, 223)
$borderColor = [System.Drawing.Color]::FromArgb(67, 74, 84)
$gold = [System.Drawing.Color]::FromArgb(230, 162, 49)
$purple = [System.Drawing.Color]::FromArgb(190, 72, 232)
$blue = [System.Drawing.Color]::FromArgb(75, 170, 255)
$green = [System.Drawing.Color]::FromArgb(60, 200, 135)
$red = [System.Drawing.Color]::FromArgb(245, 124, 103)

$titleFont = [System.Drawing.Font]::new('Segoe UI', 31, [System.Drawing.FontStyle]::Bold)
$subFont = [System.Drawing.Font]::new('Segoe UI', 15)
$labelFont = [System.Drawing.Font]::new('Segoe UI', 13, [System.Drawing.FontStyle]::Bold)
$boxTitleFont = [System.Drawing.Font]::new('Segoe UI', 21, [System.Drawing.FontStyle]::Bold)
$boxBodyFont = [System.Drawing.Font]::new('Segoe UI', 16)
$smallFont = [System.Drawing.Font]::new('Segoe UI', 13)
$brushWhite = [System.Drawing.SolidBrush]::new($white)
$brushMuted = [System.Drawing.SolidBrush]::new($muted)

function Draw-RoundRect($x, $y, $w, $h, $radius, $color, $width = 2) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $radius * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.DrawPath([System.Drawing.Pen]::new($color, $width), $path)
    $path.Dispose()
}

function Fill-RoundRect($x, $y, $w, $h, $radius, $fillColor) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $radius * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.FillPath([System.Drawing.SolidBrush]::new($fillColor), $path)
    $path.Dispose()
}

function Draw-Text($text, $font, $color, $x, $y, $w, $h, $align = 'Center') {
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = if ($align -eq 'Left') { [System.Drawing.StringAlignment]::Near } else { [System.Drawing.StringAlignment]::Center }
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, [System.Drawing.SolidBrush]::new($color), [System.Drawing.RectangleF]::new($x, $y, $w, $h), $format)
    $format.Dispose()
}

function Draw-Arrow($x1, $y1, $x2, $y2, $dashed = $false) {
    $pen = [System.Drawing.Pen]::new($lineColor, 3)
    if ($dashed) { $pen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash }
    $cap = [System.Drawing.Drawing2D.AdjustableArrowCap]::new(7, 8, $true)
    $pen.CustomEndCap = $cap
    $g.DrawLine($pen, $x1, $y1, $x2, $y2)
    $cap.Dispose(); $pen.Dispose()
}

function Draw-Component($x, $y, $w, $h, $accent, $title, $body, $badge) {
    Fill-RoundRect $x $y $w $h 18 ([System.Drawing.Color]::FromArgb(43, 39, 45, 55))
    Draw-RoundRect $x $y $w $h 18 $accent 2
    Fill-RoundRect ($x + 18) ($y + 16) 150 30 6 ([System.Drawing.Color]::FromArgb(37, 20, 22, 27))
    Draw-RoundRect ($x + 18) ($y + 16) 150 30 6 $accent 1
    Draw-Text $badge $labelFont $white ($x + 18) ($y + 16) 150 30
    $g.FillEllipse([System.Drawing.SolidBrush]::new($accent), $x + 32, $y + 62, 42, 42)
    $componentTitleFont = if ($w -lt 240) { [System.Drawing.Font]::new('Segoe UI', 17, [System.Drawing.FontStyle]::Bold) } else { $boxTitleFont }
    Draw-Text $title $componentTitleFont $white ($x + 24) ($y + 105) ($w - 48) 35
    Draw-Text $body $smallFont $muted ($x + 28) ($y + 145) ($w - 56) ($h - 160)
}

Draw-Text 'MemoryChat - Deployment Architecture' $titleFont $white 0 28 1900 50
Draw-Text 'JWT-protected conversational memory system deployed with Vercel and Render' $subFont $muted 0 83 1900 28
Draw-RoundRect 18 18 1864 1074 10 $borderColor 2

# Browser and frontend group.
Draw-RoundRect 60 190 420 560 22 $gold 2
Draw-Text 'CLIENT EXPERIENCE' $labelFont $gold 82 208 170 28 'Left'
Draw-Component 92 262 356 190 $blue 'Browser UI' "Sign in, create account, chat,`nsearch, edit, and delete memories." 'USER'
Draw-Component 92 505 356 190 $green 'Vercel Frontend' "Static Auth and Chat pages.`n/api requests are proxied to Render." 'VERCEL'

# Backend deployment group.
Draw-RoundRect 560 150 690 720 22 $gold 2
Draw-Text 'DEPLOYMENT' $labelFont $gold 582 168 130 28 'Left'
Draw-RoundRect 590 215 630 610 18 $purple 2
Draw-Text 'RENDER WEB SERVICE' $labelFont $purple 610 233 210 28 'Left'
Draw-Component 625 285 560 214 $green 'Spring Boot API' "REST endpoints for authentication,`nmemories, and chat requests." 'API'
Draw-Component 625 548 560 230 $purple 'Application Services' "JWT authentication  |  User data isolation`nMemory search, editing, and relevance scoring`nAI orchestration and memory extraction" 'SECURE CORE'

# Data and external providers group.
Draw-RoundRect 1330 190 505 560 22 $gold 2
Draw-Text 'DATA AND AI SERVICES' $labelFont $gold 1352 208 220 28 'Left'
Draw-Component 1360 262 445 170 $blue 'Render H2 Database' "Demo profile: app_users and memories.`nUser IDs enforce data ownership." 'DATABASE'
Draw-Component 1360 480 208 215 $purple 'Gemini AI' "Primary`nanswer provider" 'PRIMARY'
Draw-Component 1597 480 208 215 $red 'NVIDIA NIM' "Fallback`nanswer provider" 'FALLBACK'

# Flow arrows.
Draw-Arrow 270 452 270 505
Draw-Arrow 448 600 625 390
Draw-Arrow 905 499 905 548
Draw-Arrow 1185 390 1360 347
Draw-Arrow 1185 655 1360 588
Draw-Arrow 1185 690 1597 588

# Source / deployment footline.
Draw-Component 610 875 270 180 $blue 'GitHub Repository' "Source code and version history" 'SOURCE'
Draw-Text 'Developer pushes updates' $smallFont $muted 225 950 260 25
Draw-Arrow 485 978 610 978 $true
Draw-Text 'Vercel and Render deployments use the configured project source and environment variables.' $smallFont $muted 920 920 730 30 'Left'
Draw-Text 'Browser requests use HTTPS. JWT bearer tokens protect API routes and user-scoped memory data.' $smallFont $muted 920 965 760 30 'Left'

$output = Join-Path $PSScriptRoot '..\diagrams\MemoryChat_Architecture_Diagram.png'
$canvas.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose(); $canvas.Dispose()
Write-Output $output
