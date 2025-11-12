# Robust backend Dockerfile (avoids mvnw/.mvn conditional copy issues)
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy only the files we need (pom and source)
COPY pom.xml ./
# If you have a parent POM outside this folder, you must copy it too or set context accordingly.
COPY src ./src

# build non-interactive with system mvn (uses image's mvn)
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
