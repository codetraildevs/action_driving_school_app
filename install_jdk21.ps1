$json = Invoke-RestMethod -Uri "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse"
$link = $json[0].binary.package.link
$name = $json[0].binary.package.name
Write-Host "Downloading $name"
Write-Host "From: $link"
$out = "D:\jdk21-installer.msi"
Invoke-WebRequest -Uri $link -OutFile $out
Write-Host "Downloaded to: $out"
Write-Host "File size: $((Get-Item $out).Length / 1MB) MB"
