# ---- build stage ----
# Kebutuhan untuk Github Actions nanti (Kalo tidak perlu bisa diapus)
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar app.jar"]