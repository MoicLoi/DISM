#Requires -Version 5.1
<#
.SYNOPSIS
    组装一个可以直接上传到 GitHub Release 的产物包。

.DESCRIPTION
    产物分三类，缺一不可：

    ```text
    source      纯源码 zip（不含 build/ 等本地产物），供审阅与自行构建
    artifacts   真正能跑的东西：桌面应用、Web(js)、Web(wasm)、Android APK
    commands    构建 / 测试 / 运行 / 校验的精确命令
    ```

    只给源码不算发布，只给二进制不可审计，只给命令等于让用户自己猜。
    这个脚本把三者放进同一个目录并附上 SHA256 清单。

    ⚠ 关于默认产物

    默认只做 `createDistributable`（Windows 上是解压即用的应用目录）。
    MSI 需要本机安装 WiX Toolset 3.x，jpackage 才会成功，所以放在 `-TryMsi` 后面
    按需开启——默认开启只会让没有 WiX 的机器上打包直接失败。

    刻意不做的事：

    ```text
    不签名            没有证书，别假装签过
    不发布 :server    它目前是 Ktor 的 Hello 桩，没有发布价值
    不自动打 tag      出版本号是人的决定，不是脚本的副作用
    ```

.PARAMETER Version
    发布版本号。默认取 :core 的公共 API 版本，当前是 1.5.0。

.PARAMETER OutputRoot
    输出根目录，相对仓库根解析。默认 build/github-release。

.PARAMETER SkipBuild
    跳过 Gradle 构建，只做组装（产物已存在时用）。

.PARAMETER TryMsi
    额外尝试 Windows MSI。需要 WiX Toolset 3.x。

# `powershell` 是 Windows 自带的 5.1；这台机器上没有 `pwsh`（7.x），
# 所以文档与示例一律用 powershell，别写成 pwsh。
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/package-github-release.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/package-github-release.ps1 -SkipBuild
#>
[CmdletBinding()]
param(
    [string] $Version = '1.5.0',
    [string] $OutputRoot = 'build/github-release',
    [switch] $SkipBuild,
    [switch] $TryMsi,
    [switch] $IncludeSourceMaps
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ZipArchive / ZipArchiveMode 在 System.IO.Compression 里，
# ZipFile 在 System.IO.Compression.FileSystem 里——两个都要加载。
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repoRoot = Split-Path -Parent $PSScriptRoot
$packageName = "dism-$Version"
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $repoRoot $OutputRoot))
$packagePath = Join-Path $outputPath $packageName

function Write-Step {
    param([string] $Message)
    Write-Host ''
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-Path {
    param([string] $Path, [string] $Explanation)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "缺少 $Path`n$Explanation"
    }
}

function New-ZipFromDirectory {
    param(
        [string] $SourceDirectory,
        [string] $ZipPath
    )

    if (Test-Path -LiteralPath $ZipPath) {
        Remove-Item -LiteralPath $ZipPath -Force
    }

    $parent = Split-Path -Parent $ZipPath
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    # 为什么不用 ZipFile::CreateFromDirectory：
    # 在 .NET Framework（也就是 Windows PowerShell 5.1）上，它写进 zip 的
    # 条目名用**反斜杠**分隔。Windows 的解压工具能忍，Linux/macOS 的 unzip
    # 不认识——它会解出一堆名字里带 \ 的文件，而不是目录结构。
    # Web 产物经常被部署到 Linux 容器里，所以必须自己写条目名。
    #
    # 顺带得到两个好处：
    #   - 条目按文件名排序
    #   - 时间戳固定为 zip 纪元（1980-01-01）
    # 于是同样的输入两次打包会得到**逐字节相同**的 zip，SHA256 可复现。
    $rootName = Split-Path -Leaf $SourceDirectory
    $epoch = [DateTimeOffset]::new(1980, 1, 1, 0, 0, 0, [TimeSpan]::Zero)

    $archive = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $files = Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File -Force | Sort-Object FullName

        foreach ($file in $files) {
            $relative = $file.FullName.Substring($SourceDirectory.Length).TrimStart('\', '/')
            $entryName = $rootName + '/' + ($relative -replace '\\', '/')

            $entry = $archive.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = $epoch

            $entryStream = $entry.Open()
            try {
                $inputStream = [System.IO.File]::OpenRead($file.FullName)
                try {
                    $inputStream.CopyTo($entryStream)
                }
                finally {
                    $inputStream.Dispose()
                }
            }
            finally {
                $entryStream.Dispose()
            }
        }
    }
    finally {
        $archive.Dispose()
    }

    $sizeKb = [math]::Round((Get-Item -LiteralPath $ZipPath).Length / 1KB, 1)
    Write-Host ("    {0} ({1} KB)" -f (Split-Path -Leaf $ZipPath), $sizeKb)
}

# ---------------------------------------------------------------------------
# 0. 路径安全检查
# ---------------------------------------------------------------------------

# 下面会 Remove-Item 整个输出目录。先确认解析后的绝对路径确实在仓库内，
# 避免 OutputRoot 被写成绝对路径或 ..\.. 时删掉别的东西。
if (-not $outputPath.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "输出目录不在仓库内，拒绝操作：$outputPath"
}

if (Test-Path -LiteralPath $outputPath) {
    Write-Step "清理旧的输出目录"
    Remove-Item -LiteralPath $outputPath -Recurse -Force
}

New-Item -ItemType Directory -Path $packagePath -Force | Out-Null

# ---------------------------------------------------------------------------
# 1. 构建
# ---------------------------------------------------------------------------

if (-not $SkipBuild) {
    Write-Step "构建发布产物（Gradle）"

    $gradleTasks = @(
        ':app:desktopApp:createDistributable',
        ':app:webApp:jsBrowserDistribution',
        ':app:webApp:wasmJsBrowserDistribution',
        ':app:androidApp:assembleDebug'
    )

    if ($TryMsi) {
        $gradleTasks += ':app:desktopApp:packageMsi'
    }

    & (Join-Path $repoRoot 'gradlew.bat') @gradleTasks '--console=plain'

    if ($LASTEXITCODE -ne 0) {
        throw "Gradle 构建失败（exit $LASTEXITCODE），未生成发布包"
    }
}
else {
    Write-Step "跳过构建（-SkipBuild）"
}

# ---------------------------------------------------------------------------
# 2. 源码
# ---------------------------------------------------------------------------

Write-Step "打包源码"

$sourceStaging = Join-Path (Join-Path $outputPath 'stage') $packageName

# 这些目录是本机构建产物或 IDE 状态，进源码包只会污染审查。
$excludedDirectories = @(
    'build',
    '.gradle',
    '.kotlin',
    '.idea',
    'kotlin-js-store',
    'dist-source-for-agents',
    'node_modules',
    'captures'
)

# local.properties 里写着本机的 SDK 路径，对别人无用且泄漏本地环境。
$excludedFiles = @('local.properties')

$excludedSet = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($name in $excludedDirectories) {
    [void] $excludedSet.Add($name)
}

$excludedFileSet = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($name in $excludedFiles) {
    [void] $excludedFileSet.Add($name)
}

$sourceFiles = Get-ChildItem -LiteralPath $repoRoot -Recurse -File -Force | Where-Object {
    $relative = $_.FullName.Substring($repoRoot.Length).TrimStart('\', '/')
    $segments = $relative -split '[\\/]'

    $skip = $false

    # 任何一个路径段命中排除目录就整体跳过。
    # 含段比较，而不是只比较第一层——core\build 也要排除。
    foreach ($segment in $segments) {
        if ($excludedSet.Contains($segment)) {
            $skip = $true
            break
        }
    }

    if (-not $skip -and $excludedFileSet.Contains($_.Name)) {
        $skip = $true
    }

    -not $skip
}

foreach ($file in $sourceFiles) {
    $relative = $file.FullName.Substring($repoRoot.Length).TrimStart('\', '/')
    $target = Join-Path $sourceStaging $relative
    $targetDirectory = Split-Path -Parent $target

    if (-not (Test-Path -LiteralPath $targetDirectory)) {
        New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
    }

    Copy-Item -LiteralPath $file.FullName -Destination $target -Force
}

Write-Host ("    源码文件 {0} 个" -f $sourceFiles.Count)

New-ZipFromDirectory -SourceDirectory $sourceStaging -ZipPath (Join-Path $packagePath "source\$packageName-source.zip")

# ---------------------------------------------------------------------------
# 3. 桌面应用
# ---------------------------------------------------------------------------

Write-Step "打包桌面应用"

$desktopImage = Join-Path $repoRoot 'app\desktopApp\build\compose\binaries\main\app\me.moiclathy.dism'
Assert-Path $desktopImage "先跑 .\gradlew.bat :app:desktopApp:createDistributable"

$desktopStaging = Join-Path (Join-Path $outputPath 'stage') "$packageName-desktop"

# 直接把目录复制成目标目录（而不是 dir\*），这样 .jpackage.xml 这类隐藏文件也会带上。
Copy-Item -LiteralPath $desktopImage -Destination $desktopStaging -Recurse -Force

New-ZipFromDirectory -SourceDirectory $desktopStaging -ZipPath (Join-Path $packagePath "artifacts\desktop\$packageName-desktop-windows-x64.zip")

$msiPath = Get-ChildItem -LiteralPath (Join-Path $repoRoot 'app\desktopApp\build\compose\binaries\main\msi') -Filter '*.msi' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -ne $msiPath) {
    $msiTarget = Join-Path $packagePath "artifacts\desktop\$packageName-windows-x64.msi"
    New-Item -ItemType Directory -Path (Split-Path -Parent $msiTarget) -Force | Out-Null
    Copy-Item -LiteralPath $msiPath.FullName -Destination $msiTarget -Force
    Write-Host ("    {0}" -f $msiPath.Name)
}
elseif ($TryMsi) {
    Write-Warning "指定了 -TryMsi，但没有找到 MSI 产物；可能是本机缺少 WiX Toolset 3.x。"
}

# ---------------------------------------------------------------------------
# 4. Web（js / wasmJs）
# ---------------------------------------------------------------------------

Write-Step "打包 Web 产物"

$webTargets = @(
    @{ Name = 'js';   Folder = 'js';     Task = ':app:webApp:jsBrowserDistribution' },
    @{ Name = 'wasm'; Folder = 'wasmJs'; Task = ':app:webApp:wasmJsBrowserDistribution' }
)

foreach ($target in $webTargets) {
    $source = Join-Path $repoRoot ("app\webApp\build\dist\{0}\productionExecutable" -f $target.Folder)
    Assert-Path $source "先跑 .\gradlew.bat $($target.Task)"

    $staging = Join-Path (Join-Path $outputPath 'stage') ("{0}-web-{1}" -f $packageName, $target.Name)
    Copy-Item -LiteralPath $source -Destination $staging -Recurse -Force

    # source map 是调试用的，单个 6MB 量级，对下载者没有价值——
    # 想看源码的人本来就有源码包。默认不带，需要时用 -IncludeSourceMaps。
    if (-not $IncludeSourceMaps) {
        Get-ChildItem -LiteralPath $staging -Recurse -File -Filter '*.map' | Remove-Item -Force
    }

    New-ZipFromDirectory -SourceDirectory $staging -ZipPath (Join-Path $packagePath ("artifacts\web\{0}-web-{1}.zip" -f $packageName, $target.Name))
}

# ---------------------------------------------------------------------------
# 5. Android
# ---------------------------------------------------------------------------

Write-Step "打包 Android APK"

$apkDirectory = Join-Path $repoRoot 'app\androidApp\build\outputs\apk\debug'
Assert-Path $apkDirectory "先跑 .\gradlew.bat :app:androidApp:assembleDebug"

$apk = Get-ChildItem -LiteralPath $apkDirectory -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $apk) {
    throw "在 $apkDirectory 下没有找到 APK；:app:androidApp:assembleDebug 可能没有产出。"
}

# debug APK 未签名发布密钥，装上会提示"未知来源"。
# 文件名里写死 debug，避免用户以为这是可直接上架的正式包。
$apkTarget = Join-Path $packagePath ("artifacts\android\{0}-debug.apk" -f $packageName)
New-Item -ItemType Directory -Path (Split-Path -Parent $apkTarget) -Force | Out-Null
Copy-Item -LiteralPath $apk.FullName -Destination $apkTarget -Force
Write-Host ("    {0}" -f (Split-Path -Leaf $apkTarget))

# ---------------------------------------------------------------------------
# 6. 说明文件
# ---------------------------------------------------------------------------

Write-Step "写入说明文件"

$versionFile = Join-Path $packagePath 'VERSION.txt'
$versionLines = @(
    "version      : $Version"
    "package      : $packageName"
    "built at     : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
    "built on     : $([System.Environment]::OSVersion.VersionString)"
    "core api     : see core/src/commonMain/kotlin/me/moiclathy/dism/api/DismApiVersion.kt"
)
Set-Content -LiteralPath $versionFile -Value $versionLines -Encoding UTF8

$readmeFile = Join-Path $packagePath 'README.md'
$readmeLines = @(
    "# DISM $Version — 发布包"
    ''
    '这个目录装了三样东西：**源码**、**可运行的产物**、**需要的命令**。'
    ''
    '```text'
    "source/$packageName-source.zip                     源码，可自行构建"
    "artifacts/desktop/$packageName-desktop-windows-x64.zip   解压即用的桌面应用"
    "artifacts/web/$packageName-web-js.zip               静态站点（Kotlin/JS）"
    "artifacts/web/$packageName-web-wasm.zip             静态站点（Kotlin/Wasm）"
    "artifacts/android/$packageName-debug.apk            调试版 APK（未用发布证书签名）"
    'SHA256SUMS.txt                                      全部文件的 SHA256'
    'RELEASE-NOTES.md                                    发布说明（GitHub Release 正文）'
    'COMMANDS.md                                         构建 / 测试 / 运行 / 上传的完整命令'
    '```'
    ''
    '## 从哪开始'
    ''
    '```text'
    '只想用一下     解压 desktop 那个 zip，运行根目录下的 me.moiclathy.dism.exe'
    '想在浏览器里试 解压 web-js 那个 zip，用任意静态服务器托管'
    '想自己构建     解压 source 那个 zip，按 COMMANDS.md 第 2、3 节做'
    '想核对文件     对照 SHA256SUMS.txt'
    '```'
    ''
    '## 先说清楚'
    ''
    '```text'
    '桌面包没有代码签名     Windows 首次运行会提示"未知发布者"'
    'APK 是 debug 包        安装时提示"未知来源"'
    '没有 iOS 产物          iOS 目标在 Windows 上无法构建'
    '没有 MSI               需要 WiX Toolset 3.x'
    '```'
    ''
    '这些是限制，不是待办事项。详细情况见 RELEASE-NOTES.md 的「没验证到的」一节。'
)
Set-Content -LiteralPath $readmeFile -Value $readmeLines -Encoding UTF8

$releaseNotes = Join-Path $repoRoot 'docs\release\16.36-github-release.md'
if (Test-Path -LiteralPath $releaseNotes) {
    Copy-Item -LiteralPath $releaseNotes -Destination (Join-Path $packagePath 'RELEASE-NOTES.md') -Force
}
else {
    Write-Warning "没有找到 docs\release\16.36-github-release.md，发布包里将缺少 RELEASE-NOTES.md。"
}

$commands = Join-Path $repoRoot 'docs\release\16.36-github-release-commands.md'
if (Test-Path -LiteralPath $commands) {
    Copy-Item -LiteralPath $commands -Destination (Join-Path $packagePath 'COMMANDS.md') -Force
}
else {
    Write-Warning "没有找到 docs\release\16.36-github-release-commands.md，发布包里将缺少 COMMANDS.md。"
}

# ---------------------------------------------------------------------------
# 7. SHA256 清单
# ---------------------------------------------------------------------------

Write-Step "生成 SHA256 清单"

$artifacts = Get-ChildItem -LiteralPath $packagePath -Recurse -File | Where-Object {
    $_.Name -ne 'SHA256SUMS.txt'
} | Sort-Object FullName

$sumLines = foreach ($artifact in $artifacts) {
    $hash = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $relative = $artifact.FullName.Substring($packagePath.Length).TrimStart('\', '/') -replace '\\', '/'
    "$hash  $relative"
}

Set-Content -LiteralPath (Join-Path $packagePath 'SHA256SUMS.txt') -Value $sumLines -Encoding UTF8

# ---------------------------------------------------------------------------
# 8. 汇总
# ---------------------------------------------------------------------------

$stagingRoot = Join-Path $outputPath 'stage'
if (Test-Path -LiteralPath $stagingRoot) {
    Remove-Item -LiteralPath $stagingRoot -Recurse -Force
}

Write-Step "完成"

Get-ChildItem -LiteralPath $packagePath -Recurse -File | Sort-Object FullName | ForEach-Object {
    $relative = $_.FullName.Substring($outputPath.Length).TrimStart('\', '/')
    $kb = [math]::Round($_.Length / 1KB, 1)
    Write-Host ("    {0,-64} {1,8} KB" -f $relative, $kb)
}

Write-Host ''
Write-Host "发布包目录：$packagePath" -ForegroundColor Green
Write-Host '上传命令见包内 COMMANDS.md。' -ForegroundColor Green
