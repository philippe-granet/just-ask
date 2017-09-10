#========================
# Build
#========================
FROM maven:3.5.0-jdk-8-alpine as BUILDER

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY pom.xml /usr/src/app
# get all the downloads out of the way
RUN mvn -B -V -s /usr/share/maven/ref/settings-docker.xml help:system verify clean --fail-never
RUN mkdir -p /usr/src/app/target
RUN echo $(mvn -q \
    -Dexec.executable="echo" \
    -Dexec.args='${project.version}' \
    --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.6.0:exec) > target/project.version

COPY src /usr/src/app/src
RUN mvn -B -V -e -s /usr/share/maven/ref/settings-docker.xml help:system install -DskipTests=true -Dmaven.javadoc.skip=true

RUN cp target/just-ask-$(cat target/project.version)-jar-with-dependencies.jar target/just-ask-jar-with-dependencies.jar

#========================
# Docker image
#========================
FROM selenium/base:3.5.3-astatine
LABEL maintainer="philippe.granet@gmail.com"

USER seluser

#========================
# Docker Configuration
#========================
RUN sudo groupadd docker &&\
	sudo usermod -aG docker seluser &&\
	sudo usermod -aG staff seluser

#========================
# Just Ask library
#========================
COPY --from=BUILDER /usr/src/app/target/project.version /opt/selenium/just-ask.version
COPY --from=BUILDER /usr/src/app/target/just-ask-jar-with-dependencies.jar /opt/selenium/

#========================
# Selenium Configuration
#========================

EXPOSE 4444

# As integer, maps to "maxSession"
ENV GRID_MAX_SESSION 5
# In milliseconds, maps to "newSessionWaitTimeout"
ENV GRID_NEW_SESSION_WAIT_TIMEOUT -1
# As a boolean, maps to "throwOnCapabilityNotPresent"
ENV GRID_THROW_ON_CAPABILITY_NOT_PRESENT true
# As an integer
ENV GRID_JETTY_MAX_THREADS -1
# In milliseconds, maps to "cleanUpCycle"
ENV GRID_CLEAN_UP_CYCLE 5000
# In seconds, maps to "browserTimeout"
ENV GRID_BROWSER_TIMEOUT 0
# In seconds, maps to "timeout"
ENV GRID_TIMEOUT 30
# Debug
ENV GRID_DEBUG false

COPY generate_config \
    entry_point.sh \
    /opt/bin/
RUN sudo chmod +x /opt/bin/generate_config /opt/bin/entry_point.sh
RUN /opt/bin/generate_config > /opt/selenium/config.json

CMD ["/opt/bin/entry_point.sh"]