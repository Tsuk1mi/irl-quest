param(
    [Parameter(Mandatory=$true)][string]$Model,
    [Parameter(Mandatory=$false)][string]$Input
)

# If Input not provided as arg, read from stdin
if (-not $Input) {
    $stdin = [Console]::In.ReadToEnd()
    $Input = $stdin.Trim()
}

if (-not $Input) {
    Write-Error "No input provided"
    exit 1
}

# Call ollama run with model and input, capture stdout
$cmd = @("ollama", "run", $Model, $Input)
# Use Start-Process to capture output reliably
$process = Start-Process -FilePath "ollama" -ArgumentList @("run", $Model, $Input) -NoNewWindow -RedirectStandardOutput -RedirectStandardError -PassThru -Wait
$stdout = $process.StandardOutput.ReadToEnd()
$stderr = $process.StandardError.ReadToEnd()
$exitCode = $process.ExitCode

if ($exitCode -ne 0) {
    Write-Error "Ollama command failed: $stderr"
    exit $exitCode
}

# Write stdout raw (caller will parse)
Write-Output $stdout

