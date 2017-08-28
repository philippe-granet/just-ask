FROM openjdk:8-jdk-alpine

RUN apk add --no-cache curl tar bash
ARG MAVEN_VERSION=3.3.9
ARG USER_HOME_DIR="/root"
RUN mkdir -p /usr/share/maven && \
	curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 && \
	ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
# speed up Maven JVM a bit
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENTRYPOINT ["/usr/bin/mvn"]

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY pom.xml /usr/src/app
RUN mvn -B -U -V help:system clean verify -Dinvoker.skip=true -Dmaven.main.skip=true -Dmaven.plugin.skip=true -Dmaven.test.skip=true && \
	rm -Rf target
# copy other source files (keep in image)
COPY . /usr/src/app

RUN mvn -B -V -e help:system install -DskipTests=true -Dmaven.javadoc.skip=true