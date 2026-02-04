#!/bin/bash

# see https://api.adoptium.net/q/swagger-ui/#/Binary/getBinaryByVersion
linuxApi='https://api.adoptium.net/v3/binary/version/jdk-17.0.12%2B7/linux/x64/jre/hotspot/normal/eclipse?project=jdk'
wget -c ${linuxApi} --no-check-certificate -O jre.tar.gz
tar -xzf jre.tar.gz
mv jdk-17.0.12+7-jre jre
rm -f jre.tar.gz
