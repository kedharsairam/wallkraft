# Generates the WallKraft app icon (assets/wallkraft.ico + assets/wallkraft.png).
# Kraft brand: an accent-blue gradient squircle with a white mountain range and
# a sun disc — drawn with System.Drawing (GDI+) so the script runs anywhere on
# Windows with .NET Framework.
#
# Usage:  powershell -ExecutionPolicy Bypass -File tools\generate-icon.ps1
# Re-run any time the brand changes. The .ico is a 256x256 PNG entry (Vista+).

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$icoPath = Join-Path $root 'assets\wallkraft.ico'
$pngPath = Join-Path $root 'assets\wallkraft.png'
$size = 256

# --- draw the PNG -----------------------------------------------------------
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::Transparent)

# Rounded-square background (iOS-style squircle-ish), blue gradient.
function RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

$pad = 12
$bg = RoundedRectPath $pad $pad ($size - 2 * $pad) ($size - 2 * $pad) 56
$gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(12, 12)),
    (New-Object System.Drawing.Point(244, 244)),
    [System.Drawing.ColorTranslator]::FromHtml('#4DA3FF'),
    [System.Drawing.ColorTranslator]::FromHtml('#0055C7'))
$g.FillPath($gradient, $bg)

# Sun disc — soft white circle, top-right, slightly behind the mountains.
$sunBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, 255, 255, 255))
$g.FillEllipse($sunBrush, 148, 48, 46, 46)

# Mountain range — two overlapping peaks in white.
$mountainsBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$leftPeak = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point(36, 196)),
    (New-Object System.Drawing.Point(104, 92)),
    (New-Object System.Drawing.Point(172, 196)))
$g.FillPolygon($mountainsBrush, $leftPeak)
$rightPeak = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point(110, 196)),
    (New-Object System.Drawing.Point(168, 128)),
    (New-Object System.Drawing.Point(220, 196)))
$g.FillPolygon($mountainsBrush, $rightPeak)

# Subtle front ridge for depth.
$front = [System.Drawing.Point[]]@(
    (New-Object System.Drawing.Point(36, 196)),
    (New-Object System.Drawing.Point(84, 152)),
    (New-Object System.Drawing.Point(132, 196)))
$g.FillPolygon($mountainsBrush, $front)

$bmp.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)

# --- wrap the PNG in an ICO container ---------------------------------------
$pngMs = New-Object System.IO.MemoryStream
$bmp.Save($pngMs, [System.Drawing.Imaging.ImageFormat]::Png)
$pngBytes = $pngMs.ToArray()

$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter($fs)
try {
    $bw.Write([UInt16]0)             # reserved
    $bw.Write([UInt16]1)             # type: icon
    $bw.Write([UInt16]1)             # image count
    $bw.Write([Byte]0)               # width  (0 = 256)
    $bw.Write([Byte]0)               # height (0 = 256)
    $bw.Write([Byte]0)               # palette
    $bw.Write([Byte]0)               # reserved
    $bw.Write([UInt16]1)             # color planes
    $bw.Write([UInt16]32)            # bits per pixel
    $bw.Write([UInt32]$pngBytes.Length)  # size in bytes
    $bw.Write([UInt32]22)            # offset (6 header + 16 entry)
    $bw.Write($pngBytes)
} finally {
    $bw.Close(); $fs.Close(); $pngMs.Dispose()
}

$g.Dispose(); $bmp.Dispose()
Write-Host "Generated $icoPath and $pngPath"
