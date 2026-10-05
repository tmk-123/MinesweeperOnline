# Script to OCR all images in docs/media using Windows.Media.Ocr
Add-Type -AssemblyName System.Drawing
[Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null
[Windows.Graphics.Imaging.BitmapDecoder, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.StorageFile, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null

$engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
if ($null -eq $engine) {
    $engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromLanguage([Windows.Globalization.Language]::new("en-US"))
}

$files = Get-ChildItem "docs/media/*.png" | Sort-Object { [int]($_.BaseName -replace '\D') }
$outFile = "docs/ocr_results.txt"
"=== OCR RESULTS FOR DOCS/MEDIA ===" | Out-File -FilePath $outFile -Encoding utf8

function Await($task) {
    while (-not $task.IsCompleted) { Start-Sleep -Milliseconds 10 }
    return $task.GetResults()
}

foreach ($f in $files) {
    try {
        $path = $f.FullName
        $fileTask = [Windows.Storage.StorageFile]::GetFileFromPathAsync($path)
        $storageFile = Await($fileTask)
        $streamTask = $storageFile.OpenAsync([Windows.Storage.FileAccessMode]::Read)
        $stream = Await($streamTask)
        $decoderTask = [Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)
        $decoder = Await($decoderTask)
        $bmpTask = $decoder.GetSoftwareBitmapAsync()
        $bmp = Await($bmpTask)
        
        $ocrTask = $engine.RecognizeAsync($bmp)
        $result = Await($ocrTask)
        
        "`n========================================" | Out-File -FilePath $outFile -Append -Encoding utf8
        "IMAGE: $($f.Name) (Text lines: $($result.Lines.Count))" | Out-File -FilePath $outFile -Append -Encoding utf8
        "========================================" | Out-File -FilePath $outFile -Append -Encoding utf8
        foreach ($line in $result.Lines) {
            $line.Text | Out-File -FilePath $outFile -Append -Encoding utf8
        }
        Write-Host "Processed $($f.Name): $($result.Lines.Count) lines"
    } catch {
        Write-Host "Error processing $($f.Name): $_"
    }
}
Write-Host "All done! Saved to $outFile"
