# 在后端目录执行：先设置 Key 再启动，避免 IDE 读不到 Windows 用户变量
# 用法：.\run-dev.ps1
# 或一行：$env:FORUM_LLM_API_KEY='你的sk-密钥'; mvn spring-boot:run

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $env:FORUM_LLM_API_KEY) {
  $k = Read-Host "可选：输入 FORUM_LLM_API_KEY（直接回车则使用 application-local.yml 里的 forum.llm.api-key）"
  if ($k) { $env:FORUM_LLM_API_KEY = $k.Trim() }
}

# PowerShell 须给 -Dspring-boot... 加引号，否则会被拆成非法的「生命周期阶段」
mvn spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8"
