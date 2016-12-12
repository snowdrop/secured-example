# Introduction

This project exposes a simple REST endpoint where the service `greeting` is available at this address `http://hostname:port/greeting` and returns a json Greeting message

```
{
    "content": "Hello, World!",
    "id": 1
}

```

The id of the message is incremented for each request. To customize the message, you can pass as parameter the name of the person that you want to send your greeting.

# Build

The project bundles the Apache Tomcat 8.0.36 artifacts with SpringBoot 1.4.1.RELEASE. It can be used with the Apache Tomcat Red Hat Jar or the files
proposed by the Apache Tomcat Community project. The by default profile will use the Red Hat jar files but you can also make a test using the community files.

To build the project, use this maven command.

```
mvn clean install
```

# Launch and test

To start Spring Boot , run the following commands in order to start the maven goal of Spring Boot

```
mvn spring-boot:run
```

If the application has been launched without any error, you can access the REST endpoint exposed using curl or httpie tool

```
http http://localhost:8080/greeting
curl http://localhost:8080/greeting
```

To pass a parameter for the Greeting Service, use this HTTP request

```
http http://localhost:8080/greeting name==Charles
curl http://localhost:8080/greeting -d name=Bruno
```


# OpenShift

The Project can be deployed top of Openshift using the [minishift tool](https://github.com/minishift/minishift) who will take care to install within a Virtual machine (Virtualbox, libvirt or Xhyve) the OpenShift platform
like also a Docker daemon. For that purpose, you will first issue within a terminal the following commands.

```
minishift delete
minishift start --openshift-version=v1.3.1
eval $(minishift docker-env)
oc login --username=admin --password=admin
```

## Red Hat SSO

```
oc new-project sso
oc create -f etc/app-template.json
```

## Spring Boot secured


Next, we will use the Fabric8 Maven plugin which is a Java OpenShift/Kubernetes API able to communicate with the prlatform in order to request to build the docker image and next to create using Kubernetes
a pod from the image of our application.

A maven profile has been defined within this project to configure the Fabric8 Maven plugin

```
mvn clean fabric8:build -Popenshift -DskipTests
```

Next we can deploy the templates top of OpenShift and wait till kubernetes has created the POD

```
mvn -Popenshift fabric8:deploy -DskipTests
```

Then, you can test the service deployed in OpenShift and get a response message 

```
http $(minishift service springboot-rest --url=true)/greeting
```

To test the project against OpenShift using Arquillian, simply run this command

```
mvn test -Popenshift
```