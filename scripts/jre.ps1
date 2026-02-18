# see https://api.adoptium.net/q/swagger-ui/#/Binary/getBinaryByVersion
$winApi = 'https://api.adoptium.net/v3/binary/version/jdk-21.0.10%2B7/windows/x64/jre/hotspot/normal/eclipse?project=jdk'
Invoke-WebRequest -Uri $winApi -OutFile 'jre.zip'
Expand-Archive -Path 'jre.zip' -DestinationPath '.' -Force
Rename-Item -Path 'jdk-21.0.10+7-jre' -NewName 'jre'
Remove-Item -Path 'jre.zip' -Force
