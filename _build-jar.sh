#!/bin/bash

sbt package

if [ $? != 0 ]
then
  echo "'sbt package' failed, exiting now"
  exit 1
fi

cp target/scala-2.10/interactiveemailclient_2.10-0.1.jar InteractiveEmailClient.jar

ls -l InteractiveEmailClient.jar

echo ""
echo "Created InteractiveEmailClient.jar. Copy that file to /Users/al/Sarah/plugins/DDInteractiveEmailClient, like this:"
echo "cp InteractiveEmailClient.jar /Users/al/Sarah/plugins/DDInteractiveEmailClient"

