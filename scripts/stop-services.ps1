$projectRoot = Split-Path -Parent $PSScriptRoot

Get-CimInstance Win32_Process -Filter "name = 'java.exe'" |
    Where-Object {
        $_.CommandLine -like "*$projectRoot*" -or
        $_.CommandLine -like "*pdd-*-service*" -or
        $_.CommandLine -like "*pdd-gateway*"
    } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force
        Write-Host "Stopped Java process $($_.ProcessId)"
    }
