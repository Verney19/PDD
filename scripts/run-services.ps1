$env:NACOS_ADDR = "127.0.0.1:8848"
$env:MYSQL_HOST = "127.0.0.1"
$env:MYSQL_PORT = "3307"
$env:MYSQL_DATABASE = "pdd_flash_sale"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "123456"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:RABBITMQ_HOST = "127.0.0.1"
$env:RABBITMQ_USER = "pdd"
$env:RABBITMQ_PASSWORD = "pdd123456"

$projectRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $projectRoot "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Get-CimInstance Win32_Process -Filter "name = 'java.exe'" |
    Where-Object {
        $_.CommandLine -like "*$projectRoot*" -or
        $_.CommandLine -like "*pdd-*-service*" -or
        $_.CommandLine -like "*pdd-gateway*"
    } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force
        Write-Host "Stopped existing Java process $($_.ProcessId)"
    }

$services = @(
    @{ Name = "pdd-auth-service"; Jar = "pdd-auth-service\target\pdd-auth-service-1.0.0.jar" },
    @{ Name = "pdd-product-service"; Jar = "pdd-product-service\target\pdd-product-service-1.0.0.jar" },
    @{ Name = "pdd-order-service"; Jar = "pdd-order-service\target\pdd-order-service-1.0.0.jar" },
    @{ Name = "pdd-seckill-service"; Jar = "pdd-seckill-service\target\pdd-seckill-service-1.0.0.jar" },
    @{ Name = "pdd-lottery-service"; Jar = "pdd-lottery-service\target\pdd-lottery-service-1.0.0.jar" },
    @{ Name = "pdd-gateway"; Jar = "pdd-gateway\target\pdd-gateway-1.0.0.jar" }
)

foreach ($service in $services) {
    $jarPath = Join-Path $projectRoot $service.Jar
    $logPath = Join-Path $logDir "$($service.Name).log"
    Start-Process powershell `
        -WindowStyle Hidden `
        -WorkingDirectory $projectRoot `
        -ArgumentList "-NoExit", "-Command", "java -jar `"$jarPath`" *> `"$logPath`""
    Write-Host "Started $($service.Name), log: $logPath"
}
