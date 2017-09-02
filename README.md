[![Build Status](https://travis-ci.org/philippe-granet/just-ask.svg?branch=master)](https://travis-ci.org/philippe-granet/just-ask/builds)
[![SonarQube Tech Debt](https://sonarcloud.io/api/badges/measure?key=com.rationaleemotions%3Ajust-ask&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard?id=com.rationaleemotions%3Ajust-ask)
[![Quality gate](https://sonarcloud.io/api/badges/gate?key=com.rationaleemotions%3Ajust-ask&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard?id=com.rationaleemotions%3Ajust-ask)
[![Bugs](https://sonarcloud.io/api/badges/measure?key=com.rationaleemotions%3Ajust-ask&metric=bugs)](https://sonarqube.com/dashboard?id=com.rationaleemotions%3Ajust-ask)
[![Vulnerabilities](https://sonarcloud.io/api/badges/measure?key=com.rationaleemotions%3Ajust-ask&metric=vulnerabilities)](https://sonarqube.com/dashboard?id=com.rationaleemotions%3Ajust-ask)

# just-ask

As the name suggests, this library is eventually going to end up becoming a simple prototype that can be enhanced to 
represent an "On-demand Grid" (which explains the reason behind the name **just-ask** ).
 
## Why an On-Demand Grid ?

A static Grid (i.e., a hub and a fixed number of nodes) is a good start for setting up a remote execution infrastructure. 
But once the usage of the hub starts going up, problems start creeping up. 
Nodes go stale and start causing false failures for the tests and require constant maintenance (restart).  

Some of it can be solved by embedding a ["self healing"](https://rationaleemotions.wordpress.com/2013/01/28/building-a-self-maintaining-grid-environment/) mechanism into the hub, 
but when it comes to scaling the Grid infrastructure this also does not help a lot.

**just-ask** is an on-demand grid,  wherein there are no fixed nodes attached to the grid. 
As and when tests make hit the hub, a node is spun off, the test is routed to the node and after usage the node is 
cleaned up. The on-demand node can be a docker container that hosts a selenium node.

 ## Pre-requisites
 
 **just-ask** requires : 
 * **JDK 8**.
 * A Selenium Grid of version **3.2.0** or higher.
 * If you would like to leverage docker based on demand solution
   * Access to default Docker unix socket
     * you can refer [here](https://docs.docker.com/engine/reference/commandline/dockerd/#daemon-socket-option)
   * Or Docker Remote API enabled
     * For windows you can refer [here](http://scriptcrunch.com/enable-docker-remote-api/), 
     * For UNIX refer [here](https://docs.docker.com/engine/admin/) and 
     * For OSX refer [here](https://forums.docker.com/t/remote-api-with-docker-for-mac-beta/15639/2)


## How to use

In order to consume **just-ask** for your straight forward on-demand grid needs, following instructions need to be 
followed:
* Build the project
* Download the latest Selenium standalone jar from [here](http://www.seleniumhq.org/download/).
* Now create a configuration JSON file as shown below :
```json
{
  "port": 4444,
  "newSessionWaitTimeout": -1,
  "servlets": [
    "com.rationaleemotions.servlets.JustAskServlet"
  ],
  "withoutServlets": [
  ],
  "capabilityMatcher": "org.openqa.grid.internal.utils.DefaultCapabilityMatcher",
  "throwOnCapabilityNotPresent": true,
  "cleanUpCycle": 5000,
  "role": "hub",
  "debug": false,
  "browserTimeout": 0,
  "timeout": 1800,
  "justAsk": {
    "dockerRestApiUri": "unix:///var/run/docker.sock",
    "localhost": "0.0.0.0",
    "maxSession": 5,
    "browsers": [
      {
        "browser": "chrome",
        "defaultVersion": "59.0.3071.115",
        "versions": [
          {
            "version": "58.0.3029.81",
            "implementation": "com.rationaleemotions.server.DockerBasedSeleniumServer",
            "target": {
              "image": "selenium/standalone-chrome:3.4.0-chromium",
              "port": "4444",
              "volumes": [
                "/dev/shm:/dev/shm"
              ]
            }
          },
          {
            "version": "59.0.3071.115",
            "implementation": "com.rationaleemotions.server.DockerBasedSeleniumServer",
            "target": {
              "image": "selenium/standalone-chrome:3.4.0-einsteinium",
              "port": "4444",
              "volumes": [
                "/dev/shm:/dev/shm"
              ]
            }
          }
        ]
      },
      {
        "browser": "firefox",
        "defaultVersion": "54.0",
        "versions": [
          {
            "version": "52.0.2",
            "implementation": "com.rationaleemotions.server.DockerBasedSeleniumServer",
            "target": {
              "image": "selenium/standalone-firefox:3.4.0-actinium",
              "port": "4444",
              "shmSize": "2g"
            }
          },
          {
            "version": "53.0",
            "implementation": "com.rationaleemotions.server.DockerBasedSeleniumServer",
            "target": {
              "image": "selenium/standalone-firefox:3.4.0-dysprosium",
              "port": "4444",
              "shmSize": "2g"
            }
          },
          {
            "version": "54.0",
            "implementation": "com.rationaleemotions.server.DockerBasedSeleniumServer",
            "target": {
              "image": "selenium/standalone-firefox:3.4.0-einsteinium",
              "port": "4444",
              "shmSize": "2g"
            }
          }
        ]
      }
    ]
  }
}

```
* Start the On-demand Grid using the below command:

```
java -cp selenium-server-standalone-3.5.1.jar:just-ask-<VERSION>-jar-with-dependencies.jar \
org.openqa.grid.selenium.GridLauncherV3 -role hub -hubConfig config.json
```

* That's about it. The On-demand Grid is now ready for use.

## Understanding the JSON configuration file.
The meaning of each of the attributes of the JSON file is as below :

* `dockerRestApiUri` - Represents the Docker Rest API URI (could be `unix:///var/run/docker.sock` or `http://192.168.43.130:2375`).
* `localhost` - Represents the hostname of the machine on which the Docker Daemon is running on (Its safe to leave 
its value as `0.0.0.0` )
* `maxSession` - Represents the maximum number of concurrent sessions that can be supported by the On-demand Hub 
after which new test session requests will be queued.
* `mapping` - Represents a set of key-value pairs wherein `browser` represents the `browser flavor` and `target` 
represents the name of the docker image that is capable of supporting the respective `browser`. The `target` may not 
be relevant to all `implementation` values (for e.g., the `target` is currently relevant ONLY for `docker` based 
on-demand nodes.)

### Understanding the relevance of `implementation`
**just-ask** currently supports two implementation flavors :

* `com.rationaleemotions.server.DockerBasedSeleniumServer` - Indicates that for the browser in question, you would like
 to leverage the Docker way of spinning off nodes on demand.
*  `com.rationaleemotions.server.JvmBasedSeleniumServer` - Indicates that for the browser in question, you would like
 to leverage the JVM way of spinning off nodes on demand (i.e., the on-demand node would be a new JVM process.)

## How to customize and use.

In-case you would like to wire in your own Custom Server implementation, following is how it can be done :

**just-ask** is a [Maven](https://maven.apache.org/guides/getting-started/) artifact. In order to 
consume it, you merely need to add the following as a dependency in your pom file.

```xml
<dependency>
    <groupId>com.rationaleemotions</groupId>
    <artifactId>just-ask</artifactId>
    <version>1.0.2</version>
</dependency>
```

Now that you have added the above as a maven dependency, you build your own implementation of the Server, by 
implementing the interface `com.rationaleemotions.server.ISeleniumServer`.

After that, you can wire in your implementation via the `implementation` attribute in the JSON configuration file

## Building the code on your own

In order to get started with using this library here are the set of instructions that can be followed :
 
 * Build the code using `mvn clean package`
 * Drop the built jar (you will find two jars, so please make sure you pick up the uber jar which would have its name
  around something like this `just-ask-<VERSION>-jar-with-dependencies.jar` ) in the directory that contains the 
  selenium server standalone.
 * Start the selenium hub using the command `java -cp selenium-server-standalone-3.5.1.jar:just-ask-<VERSION>-jar-with-dependencies.jar org.openqa.grid.selenium.GridLauncherV3 -role hub -hubConfig config.json`

 Now the **On-demand Grid** is ready for use.
