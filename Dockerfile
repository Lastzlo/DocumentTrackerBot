FROM openjdk:17
ADD target/docTrackerBot-0.0.1-SNAPSHOT.jar docTrackerBot-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","docTrackerBot-0.0.1-SNAPSHOT.jar"]
