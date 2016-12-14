FROM openjdk:8

RUN mkdir /opt/springboot
ADD secured-springboot-rest-1.0-SNAPSHOT.jar /opt/springboot

WORKDIR /opt/springboot
CMD ["java", "-jar", "secured-springboot-rest-1.0-SNAPSHOT.jar"]
