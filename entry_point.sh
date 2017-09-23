#!/bin/bash

echo "========================================================"
echo "Timezone Configuration"
echo "========================================================"
echo "${TZ}" | sudo tee /etc/timezone
sudo sh -c 'dpkg-reconfigure --frontend noninteractive tzdata 2>&1'
date
echo ""

echo "========================================================"
echo "Fix permissions"
echo "========================================================"
sudo chown -R seluser:seluser /opt/selenium/

echo "========================================================"
echo "Selenium Configuration"
echo "========================================================"
ROOT=/opt/selenium

if [ "$NODE_DEBUG" = "true" ]; then
  export NODE_DEBUG_IMAGE="-debug"
else
  export NODE_DEBUG_IMAGE=""
fi

if [ ! -z "$HUB_CONFIG" ]; then
  echo "using provided hub configuration: ${HUB_CONFIG}"
else
  echo "generate hub configuration..."
  HUB_CONFIG=$ROOT/config.json
  /opt/bin/generate_config >$HUB_CONFIG
fi

echo "starting selenium hub with configuration:"
cat $HUB_CONFIG

if [ ! -z "$SE_OPTS" ]; then
  echo "appending selenium options: ${SE_OPTS}"
fi

function shutdown {
    echo "shutting down hub.."
    kill -s SIGTERM $NODE_PID
    wait $NODE_PID
    echo "shutdown complete"
}

echo "========================================================"
echo "java version"
echo "========================================================"
java -version 2>&1
echo ""

echo "========================================================"
echo "Start Hub ..."
echo "========================================================"
java ${GRID_JAVA_OPTS} \
  -cp $ROOT/just-ask-jar-with-dependencies.jar:$ROOT/selenium-server-standalone.jar \
  org.openqa.grid.selenium.GridLauncherV3 \
  -role hub \
  -hubConfig $HUB_CONFIG \
  ${SE_OPTS} &
NODE_PID=$!

trap shutdown SIGTERM SIGINT
wait $NODE_PID