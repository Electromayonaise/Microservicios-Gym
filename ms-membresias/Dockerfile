FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q package -DskipTests

# jre-jammy (glibc), not jre-alpine (musl): Kafka Streams' RocksDB state
# store loads a native JNI library that requires glibc's libstdc++.so.6.
# On Alpine the Streams client crashed on startup with UnsatisfiedLinkError
# while the container's unrelated HTTP healthcheck stayed green (see commit
# f65264b) -- switching this back to alpine silently kills Flujo 2's
# windowed aggregation.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
