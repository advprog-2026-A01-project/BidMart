# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1. Copy file wrapper & config gradle dulu (berguna untuk docker caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./

# 2. Beri permission execute ke gradlew
RUN chmod +x gradlew

# 3. Copy source code backend
COPY src/ src/

# 4. Build JAR menggunakan wrapper
RUN ./gradlew clean bootJar --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar app.jar"]