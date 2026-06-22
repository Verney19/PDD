$projectRoot = Split-Path -Parent $PSScriptRoot
$webRoot = Join-Path $projectRoot "pdd-web"
$logDir = Join-Path $projectRoot "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$existing = Get-CimInstance Win32_Process -Filter "name = 'python.exe'" |
    Where-Object { $_.CommandLine -like "*http.server*5173*" }

if ($existing) {
    Write-Host "pdd-web is already running at http://localhost:5173"
    return
}

$python = @"
import os
import subprocess
import sys

web_root = r"$webRoot"
log_dir = r"$logDir"
stdout = open(os.path.join(log_dir, "pdd-web.out.log"), "ab", buffering=0)
stderr = open(os.path.join(log_dir, "pdd-web.err.log"), "ab", buffering=0)
flags = 0
if os.name == "nt":
    flags = subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.DETACHED_PROCESS | subprocess.CREATE_NO_WINDOW
subprocess.Popen(
    [sys.executable, "-m", "http.server", "5173"],
    cwd=web_root,
    stdout=stdout,
    stderr=stderr,
    stdin=subprocess.DEVNULL,
    creationflags=flags,
)
"@

$python | python -
Write-Host "Started pdd-web at http://localhost:5173"
