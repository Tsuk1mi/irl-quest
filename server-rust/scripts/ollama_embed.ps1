param(
    [Parameter(Mandatory=$true)][string]$Model,
    [Parameter(Mandatory=$false)][string[]]$Inputs
)

# If Inputs not provided as args, read from stdin (one-per-line or JSON array)
if (-not $Inputs -or $Inputs.Count -eq 0) {
    $stdin = [Console]::In.ReadToEnd().Trim()
    if ($stdin -eq "") {
        Write-Error "No input provided"
        exit 1
    }
    # Try parse as JSON array
    try {
        $maybe = ConvertFrom-Json $stdin -ErrorAction Stop
        if ($maybe -is [System.Array]) { $Inputs = $maybe }
        else { $Inputs = @($stdin) }
    } catch {
        # treat stdin as raw text; split lines
        $Inputs = $stdin -split "\r?\n" | Where-Object { $_ -ne "" }
    }
}

# Build a single instruction asking model to output JSON array of embeddings
# We'll try asking model to return JSON array of arrays of floats; accuracy depends on model behavior
$promptBuilder = "For each input string provided, output a JSON array of 64 numeric float values (embedding) for that string.\nDo not output any other text. Output ONLY a single JSON array-of-arrays where each element corresponds to an input. Inputs:\n"
foreach ($i in $Inputs) { $promptBuilder += "- $i\n" }

# Execute ollama run
$process = Start-Process -FilePath "ollama" -ArgumentList @("run", $Model, $promptBuilder) -NoNewWindow -RedirectStandardOutput -RedirectStandardError -PassThru -Wait
$stdout = $process.StandardOutput.ReadToEnd()
$stderr = $process.StandardError.ReadToEnd()
$exitCode = $process.ExitCode
if ($exitCode -ne 0) {
    Write-Error "Ollama embed command failed: $stderr"
    exit $exitCode
}

# Try parse stdout as JSON
try {
    $parsed = ConvertFrom-Json $stdout -ErrorAction Stop
    # If parsed is array-of-arrays, write back the JSON
    $jsonOut = ConvertTo-Json $parsed -Depth 5
    Write-Output $jsonOut
} catch {
    # If parsing failed, return raw stdout but with warning exit code
    Write-Output $stdout
    Write-Warning "Could not parse model output as JSON; raw output returned."
}

