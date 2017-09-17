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
CONF=$ROOT/config.json

/opt/bin/generate_config >$CONF

echo "starting selenium hub with configuration:"
cat $CONF

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
java -server \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap \
  -XX:MaxRAMFraction=1 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XshowSettings:vm \
  -Dsun.net.inetaddr.ttl=60 \
  ${JAVA_OPTS} \
  -cp $ROOT/just-ask-jar-with-dependencies.jar:$ROOT/selenium-server-standalone.jar \
  org.openqa.grid.selenium.GridLauncherV3 \
  -role hub \
  -hubConfig $CONF \
  ${SE_OPTS} &
NODE_PID=$!

trap shutdown SIGTERM SIGINT
wait $NODE_PID