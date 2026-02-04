#!/bin/bash

APP_NAME=javafx-pasori-if.jar

tpid=$(ps -ef|grep $APP_NAME|grep -v grep|grep -v kill|awk '{print $2}')
if [ ${tpid} ]; then
echo 'Stop Process...'
kill -2 $tpid
fi

# 再次查看进程是否已结束
tpid=$(ps -ef|grep $APP_NAME|grep -v grep|grep -v kill|awk '{print $2}')
if [ ${tpid} ]; then
echo 'Stop Process...'
kill -2 $tpid
else
echo 'Stop Process Successfully!'
echo 'start Process...'
if [ -f "./jre/bin/java" ];then
  nohup jre/bin/java -jar $APP_NAME > nohup.out &
else
  nohup java -jar $APP_NAME > nohup.out &
fi
fi
