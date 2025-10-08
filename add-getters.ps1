# PowerShell script to add JavaBean-style getters to event classes

function Add-JavaBeanGetters {
    param(
        [string]$FilePath
    )

    $content = Get-Content -Path $FilePath -Raw

    # Skip if already has JavaBean-style getters
    if ($content -match "// JavaBean-style getters") {
        Write-Host "Skipping $FilePath - already has JavaBean-style getters"
        return
    }

    # Find all record-style accessors and generate JavaBean-style getters
    $lines = Get-Content -Path $FilePath
    $newContent = @()
    $inAccessors = $false
    $accessors = @()

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $newContent += $line

        # Detect record-style accessor pattern
        if ($line -match '^\s+public\s+(\w+(?:<[\w<>,\s]+>)?)\s+(\w+)\(\)\s*\{' -and $lines[$i+1] -match '^\s+return\s+(\w+);') {
            $returnType = $matches[1]
            $methodName = $matches[2]
            $fieldName = ($lines[$i+1] -match 'return\s+(\w+);') ? $matches[1] : $null

            if ($fieldName) {
                $accessors += @{
                    ReturnType = $returnType
                    MethodName = $methodName
                    FieldName = $fieldName
                }
            }
        }

        # Check if we're at the end of the class
        if ($line -match '^\}$' -and $accessors.Count -gt 0) {
            # Remove the closing brace
            $newContent = $newContent[0..($newContent.Count-2)]

            # Add comment
            $newContent += ""
            $newContent += "    // JavaBean-style getters"

            # Add JavaBean-style getters
            foreach ($accessor in $accessors) {
                $getterName = "get" + $accessor.MethodName.Substring(0,1).ToUpper() + $accessor.MethodName.Substring(1)

                # Handle boolean fields
                if ($accessor.ReturnType -eq "boolean" -and $accessor.MethodName -match '^is') {
                    $getterName = $accessor.MethodName.Substring(0,1).ToUpper() + $accessor.MethodName.Substring(1)
                }

                $newContent += "    public $($accessor.ReturnType) $getterName() {"
                $newContent += "        return $($accessor.FieldName);"
                $newContent += "    }"
                $newContent += ""
            }

            # Add closing brace back
            $newContent += "}"

            # Clear accessors for next class
            $accessors = @()
        }
    }

    # Write back to file
    $newContent | Set-Content -Path $FilePath
    Write-Host "Updated $FilePath"
}

# Get all event files from the specified directories
$eventFiles = @()
$eventFiles += Get-ChildItem -Path "C:\Users\Thomas\Documents\work\haven\backend\modules\client-profile\src\main\java\org\haven\clientprofile\domain" -Recurse -Filter "*.java" | Where-Object { $_.FullName -match "Event" -or ($_.FullName -match "\\events\\") }
$eventFiles += Get-ChildItem -Path "C:\Users\Thomas\Documents\work\haven\backend\modules\program-enrollment\src\main\java\org\haven\programenrollment\domain\events" -Recurse -Filter "*.java"
$eventFiles += Get-ChildItem -Path "C:\Users\Thomas\Documents\work\haven\backend\modules\case-mgmt\src\main\java\org\haven\casemgmt\domain" -Recurse -Filter "*.java" | Where-Object { $_.FullName -match "\\events\\" }
$eventFiles += Get-ChildItem -Path "C:\Users\Thomas\Documents\work\haven\backend\modules\service-delivery\src\main\java\org\haven\servicedelivery\domain\events" -Recurse -Filter "*.java"

foreach ($file in $eventFiles) {
    Add-JavaBeanGetters -FilePath $file.FullName
}

Write-Host "Done! Processed $($eventFiles.Count) files."
