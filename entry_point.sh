#!/bin/bash

#========================
# Timezone Configuration
#========================
echo "setting timezone"
echo "${TZ}" | sudo tee /etc/timezone
sudo dpkg-reconfigure --frontend noninteractive tzdata

#========================
# Fix permissions
#========================
sudo chown -R seluser:seluser /opt/selenium/

#========================
# Selenium Configuration
#========================
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

java ${JAVA_OPTS} \
  -cp $ROOT/just-ask-jar-with-dependencies.jar:$ROOT/selenium-server-standalone.jar \
  org.openqa.grid.selenium.GridLauncherV3 \
  -role hub \
  -hubConfig $CONF \
  ${SE_OPTS} &
NODE_PID=$!

trap shutdown SIGTERM SIGINT
wait $NODE_PID