<#
.SYNOPSIS
    End-to-end demo for the Knitting AI proofs of concept (Assignment A + B).

.DESCRIPTION
    Runs a scripted walkthrough that mirrors the video checklist in the brief:
      Assignment A - trustworthy calculations, routing, a graceful refusal,
                     JSON provenance, and the 16-question evaluation.
      Assignment B - offline fallback demo (always), plus an optional live
                     image-generation run, then points at the HTML gallery.

    The apps are built once into runnable jars and invoked with java, so the
    demo is fast and command-line arguments (including apostrophes) are safe.

    All numbers in Assignment A come from deterministic Java calculators.
    Assignment B runs fully offline with no keys; the live path is optional.

.PARAMETER Live
    Also run Assignment B against the real image provider (uses .env / env vars
    and consumes provider quota). Without this switch, only the offline
    fallback demo runs.

.PARAMETER Pause
    Wait for Enter between steps - useful when screen-recording.

.PARAMETER SkipTests
    Skip the unit-test steps and only run the live/CLI demonstrations.

.PARAMETER Open
    Open the generated Assignment B gallery in the default browser at the end.

.EXAMPLE
    .\demo.ps1
.EXAMPLE
    .\demo.ps1 -Pause -Open
.EXAMPLE
    .\demo.ps1 -Live -Open
#>
[CmdletBinding()]
param(
    [switch]$Live,
    [switch]$Pause,
    [switch]$SkipTests,
    [switch]$Open
)

$ErrorActionPreference = 'Stop'
$repoRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Set-Location $repoRoot

$assistantJar = Join-Path $repoRoot 'assignment-a\target\knitting-assistant.jar'
$swatchJar    = Join-Path $repoRoot 'assignment-b\target\swatch-preview.jar'

function Write-Section([string]$title) {
    Write-Host ''
    Write-Host ('=' * 78) -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host ('=' * 78) -ForegroundColor Cyan
    if ($Pause) {
        Read-Host '  (press Enter to run this step)' | Out-Null
    }
}

function Write-Cmd([string]$text) {
    Write-Host "> $text" -ForegroundColor DarkGray
}

function Invoke-Checked([scriptblock]$Block, [string]$Label) {
    & $Block
    if ($LASTEXITCODE -ne 0) { throw "$Label failed with exit code $LASTEXITCODE" }
}

function Ask([string]$question, [switch]$Json) {
    Write-Host "Q: $question" -ForegroundColor Yellow
    if ($Json) {
        Write-Cmd "java -jar assignment-a\target\knitting-assistant.jar --json `"$question`""
        Invoke-Checked { & java -jar $assistantJar --json $question } 'assistant'
    } else {
        Write-Cmd "java -jar assignment-a\target\knitting-assistant.jar `"$question`""
        Invoke-Checked { & java -jar $assistantJar $question } 'assistant'
    }
    Write-Host ''
}

Write-Host ''
Write-Host 'KNITTING AI - DEMO WALKTHROUGH' -ForegroundColor Green
Write-Host "Repo: $repoRoot"
Write-Host ("Live image generation: " + $(if ($Live) { 'ENABLED (uses provider quota)' } else { 'offline fallback only' }))

# --- Optional tests, then build both runnable jars ----------------------------
if (-not $SkipTests) {
    Write-Section 'Unit tests - Assignment A and Assignment B'
    Write-Cmd 'mvn -q test'
    Invoke-Checked { & mvn -q test } 'tests'
}

Write-Section 'Build runnable jars (fast, self-contained)'
Write-Cmd 'mvn -q -DskipTests package'
Invoke-Checked { & mvn -q -DskipTests package } 'package'
if (-not (Test-Path $assistantJar)) { throw "Missing $assistantJar" }
if (-not (Test-Path $swatchJar))    { throw "Missing $swatchJar" }

# =============================================================================
# ASSIGNMENT A - AI ASSISTANT WITH TRUSTWORTHY NUMBERS
# =============================================================================
Write-Section 'Assignment A - yarn quantity question (numbers come from Java code)'
Ask 'How much DK yarn for a 50 x 60cm blanket in stockinette?'

Write-Section 'Assignment A - needle size question'
Ask 'What needle size for worsted yarn for a balanced scarf?'

Write-Section 'Assignment A - tension troubleshooting question'
Ask "My swatch is 24 stitches per 10cm but the pattern says 22. What's wrong?"

Write-Section 'Assignment A - a question it must DECLINE rather than guess'
Ask 'Which sweater pattern is the prettiest?'

Write-Section 'Assignment A - JSON output (shows calculation + values provenance)'
Ask 'What needle size for worsted yarn for a balanced scarf?' -Json

Write-Section 'Assignment A - evaluation suite (16 questions: routing, values, latency)'
Write-Cmd 'java -cp assignment-a\target\knitting-assistant.jar com.knittingai.assistant.AssignmentEvaluator'
& java -cp $assistantJar com.knittingai.assistant.AssignmentEvaluator
$evalExit = $LASTEXITCODE
Write-Host "Evaluator exit code: $evalExit (0 = all routing and values correct)"

# =============================================================================
# ASSIGNMENT B - AI KNIT SWATCH PREVIEW
# =============================================================================
Write-Section 'Assignment B - OFFLINE demo (provider failure -> Java2D fallback, cache hit)'
$offlineOut = 'artifacts-demo'
Write-Cmd "java -jar assignment-b\target\swatch-preview.jar --output $offlineOut"
Invoke-Checked { & java -jar $swatchJar --output $offlineOut } 'swatch offline'
$galleryToOpen = Join-Path $repoRoot "$offlineOut\gallery.html"

if ($Live) {
    Write-Section 'Assignment B - LIVE demo (real image provider, e.g. FLUX.2-pro)'
    $liveOut = 'artifacts-live'
    Write-Cmd "java -jar assignment-b\target\swatch-preview.jar --live --output $liveOut"
    Invoke-Checked { & java -jar $swatchJar --live --output $liveOut } 'swatch live'
    $galleryToOpen = Join-Path $repoRoot "$liveOut\gallery.html"
}

# --- Summary ------------------------------------------------------------------
Write-Section 'Demo complete'
Write-Host 'Assignment A: deterministic answers, a refusal, JSON provenance, 16-case eval.' -ForegroundColor Green
Write-Host "Assignment B: offline fallback demo written to .\$offlineOut" -ForegroundColor Green
if ($Live) {
    Write-Host 'Assignment B: live images written to .\artifacts-live' -ForegroundColor Green
}
Write-Host ''
Write-Host "Open the gallery:  $galleryToOpen"

if ($Open -and (Test-Path $galleryToOpen)) {
    Invoke-Item $galleryToOpen
}
