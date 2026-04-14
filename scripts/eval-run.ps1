#Requires -Version 5.1
<#
  Eval runner: reads eval/cases.json, POSTs to baseUrl, checks expect.
  Usage (start Spring Boot with HUNYUAN_API_KEY set):
    cd <project-root>
    powershell -ExecutionPolicy Bypass -File scripts/eval-run.ps1
  Optional base URL:
    powershell -File scripts/eval-run.ps1 -BaseUrl http://127.0.0.1:8080
  If APIs require login (copy opaque token from /api/auth/login response field token):
    powershell -File scripts/eval-run.ps1 -ApiToken <uuid-or-token-string>
  NOTE: User-visible strings are ASCII-only so Windows PowerShell 5.1 parses this file
  correctly even when saved as UTF-8 without BOM.
#>
param(
    [string] $BaseUrl = "",
    [string] $CasesFile = "",
    [string] $ApiToken = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $CasesFile) { $CasesFile = Join-Path $root "eval\cases.json" }

$raw = Get-Content -LiteralPath $CasesFile -Encoding UTF8 -Raw
$cfg = $raw | ConvertFrom-Json
$appBase = if ($BaseUrl) { $BaseUrl.TrimEnd('/') } else { $cfg.baseUrl.TrimEnd('/') }

function Test-ExpectOne {
    param($obj, $expect, [string] $Label)
    $errs = @()
    if ($null -eq $obj) {
        return @("$Label : response JSON is null")
    }
    if ($expect.conversationIdRequired -eq $true) {
        $cid = $obj.conversationId
        if (-not $cid -or ($cid.ToString().Trim().Length -lt 8)) {
            $errs += "$Label : conversationId missing or too short"
        }
    }
    if ($null -ne $expect.answerMinLength) {
        $a = $obj.answer
        if (-not $a -or $a.ToString().Length -lt [int]$expect.answerMinLength) {
            $errs += "$Label : answer length below $($expect.answerMinLength)"
        }
    }
    if ($expect.stepsUsedMin -ne $null) {
        $s = $obj.stepsUsed
        if ($null -eq $s -or [int]$s -lt [int]$expect.stepsUsedMin) {
            $errs += "$Label : stepsUsed=$s want min $($expect.stepsUsedMin)"
        }
    }
    if ($expect.stepsUsedMax -ne $null) {
        $s = $obj.stepsUsed
        if ($null -ne $s -and [int]$s -gt [int]$expect.stepsUsedMax) {
            $errs += "$Label : stepsUsed=$s want max $($expect.stepsUsedMax)"
        }
    }
    $ans = if ($obj.answer) { $obj.answer.ToString() } else { "" }
    if ($expect.answerContainsAll) {
        foreach ($sub in $expect.answerContainsAll) {
            if ($ans.IndexOf($sub, [StringComparison]::Ordinal) -lt 0) {
                $errs += "$Label : answer missing substring: $sub"
            }
        }
    }
    if ($expect.answerContainsAny) {
        $hit = $false
        foreach ($sub in $expect.answerContainsAny) {
            if ($ans.IndexOf($sub, [StringComparison]::Ordinal) -ge 0) { $hit = $true; break }
        }
        if (-not $hit) {
            $errs += "$Label : answer missing any of: $($expect.answerContainsAny -join ', ')"
        }
    }
    return $errs
}

$failed = 0
$passed = 0

function Get-EvalHeaders {
    $h = @{ }
    if ($ApiToken -and $ApiToken.Trim().Length -gt 0) {
        $h["Authorization"] = "Bearer $($ApiToken.Trim())"
    }
    return $h
}

foreach ($c in $cfg.cases) {
    $id = $c.id
    if (-not $id) { continue }

    if ($c.turns) {
        Write-Host "== $id $($c.name) [multi-turn]" -ForegroundColor Cyan
        $conv = ""
        $turnIdx = 0
        $seqOk = $true
        foreach ($t in $c.turns) {
            $turnIdx++
            $body = @{ message = $t.message; conversationId = $conv } | ConvertTo-Json -Compress
            $uri = "$appBase$($c.path)"
            try {
                $resp = Invoke-WebRequest -Uri $uri -Method POST -Body $body -ContentType "application/json; charset=utf-8" -Headers (Get-EvalHeaders) -UseBasicParsing
            } catch {
                Write-Host "  FAIL turn $turnIdx : $_" -ForegroundColor Red
                $seqOk = $false
                break
            }
            if ($resp.StatusCode -ne 200) {
                Write-Host "  FAIL turn $turnIdx : HTTP $($resp.StatusCode)" -ForegroundColor Red
                $seqOk = $false
                break
            }
            $obj = $resp.Content | ConvertFrom-Json
            if ($obj.conversationId) { $conv = $obj.conversationId.ToString() }
            $errs = Test-ExpectOne $obj $t.expect "turn $turnIdx"
            if ($errs.Count -gt 0) {
                foreach ($e in $errs) { Write-Host "  FAIL $e" -ForegroundColor Red }
                if ($obj.answer) {
                    $prev = $obj.answer.ToString()
                    if ($prev.Length -gt 320) { $prev = $prev.Substring(0, 320) + "..." }
                    Write-Host "  answer preview: $prev" -ForegroundColor DarkGray
                }
                $seqOk = $false
                break
            }
            Write-Host "  ok turn $turnIdx" -ForegroundColor Green
        }
        if ($seqOk) { $passed++ } else { $failed++ }
        continue
    }

    Write-Host "== $id $($c.name)" -ForegroundColor Cyan
    $bodyObj = @{
        message          = $c.message
        conversationId   = if ($null -ne $c.conversationId) { $c.conversationId } else { "" }
    }
    $body = $bodyObj | ConvertTo-Json -Compress
    $uri = "$appBase$($c.path)"
    try {
        $resp = Invoke-WebRequest -Uri $uri -Method POST -Body $body -ContentType "application/json; charset=utf-8" -Headers (Get-EvalHeaders) -UseBasicParsing
    } catch {
        Write-Host "  FAIL HTTP: $_" -ForegroundColor Red
        $failed++
        continue
    }
    $wantStatus = if ($c.expect.httpStatus) { [int]$c.expect.httpStatus } else { 200 }
    if ([int]$resp.StatusCode -ne $wantStatus) {
        Write-Host "  FAIL HTTP status $($resp.StatusCode) want $wantStatus" -ForegroundColor Red
        $failed++
        continue
    }
    try {
        $obj = $resp.Content | ConvertFrom-Json
    } catch {
        Write-Host "  FAIL JSON: $_" -ForegroundColor Red
        $failed++
        continue
    }
    $errs = Test-ExpectOne $obj $c.expect "case"
    if ($errs.Count -gt 0) {
        foreach ($e in $errs) { Write-Host "  FAIL $e" -ForegroundColor Red }
        if ($obj.answer) {
            $prev = $obj.answer.ToString()
            if ($prev.Length -gt 320) { $prev = $prev.Substring(0, 320) + "..." }
            Write-Host "  answer preview: $prev" -ForegroundColor DarkGray
        }
        $failed++
        continue
    }
    Write-Host "  PASS" -ForegroundColor Green
    $passed++
}

Write-Host ""
Write-Host "done: passed=$passed failed=$failed" -ForegroundColor $(if ($failed -eq 0) { 'Green' } else { 'Yellow' })
if ($failed -gt 0) { exit 1 }
