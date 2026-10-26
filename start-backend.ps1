# ClassPulse 后端启动脚本（MySQL profile）
# 用法：  .\start-backend.ps1
# 如已设置 $env:DB_PASSWORD 则不再提示输入密码

$ErrorActionPreference = 'Stop'
$root = 'f:\deepseek\mjw\mjw'

# 1. 依赖环境
$env:JAVA_HOME = 'C:\Users\LENOVO\.jdks\openjdk-23.0.2'
$mvn = 'C:\Users\LENOVO\apache-maven\apache-maven-3.9.6\bin\mvn.cmd'

# 2. 数据库连接参数（有默认值，按需修改）
$env:APP_PROFILE = 'mysql'
$env:DB_HOST     = if ($env:DB_HOST) { $env:DB_HOST } else { '127.0.0.1' }
$env:DB_PORT     = if ($env:DB_PORT) { $env:DB_PORT } else { '3306' }
$env:DB_NAME     = if ($env:DB_NAME) { $env:DB_NAME } else { 'classpulse' }
$env:DB_USER     = if ($env:DB_USER) { $env:DB_USER } else { 'root' }

# 3. 密码：直接写在 backend/src/main/resources/application.yml 的 password 行
#    （形如 password: ${DB_PASSWORD:你的密码}），此处不再提示输入。
#    如已设置 $env:DB_PASSWORD，则会覆盖配置文件中的值。
if (-not $env:DB_PASSWORD) {
    $yml = Join-Path $root 'backend\src\main\resources\application.yml'
    if ((Select-String -Path $yml -Pattern 'DB_PASSWORD:CHANGE_ME' -Quiet)) {
        Write-Host "警告: application.yml 里 password 仍是 CHANGE_ME，请先填入真实密码。" -ForegroundColor Yellow
        return
    }
}

Write-Host "启动后端: profile=$env:APP_PROFILE db=$($env:DB_USER)@$($env:DB_HOST):$($env:DB_PORT)/$env:DB_NAME" -ForegroundColor Cyan

# 4. 启动
Set-Location (Join-Path $root 'backend')
& $mvn spring-boot:run
