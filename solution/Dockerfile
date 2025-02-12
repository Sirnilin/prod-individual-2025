FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon --refresh-dependencies || true

COPY src src
RUN ./gradlew bootJar -x test

RUN mkdir -p build/dependency && \
    JAR_FILE=$(ls build/libs/*-SNAPSHOT.jar | grep -v plain | head -n 1) && \
    cd build/dependency && \
    jar -xf ../libs/$(basename $JAR_FILE)

FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp

ARG DEPENDENCY=/workspace/app/build/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENV SERVER_PORT=8080

CMD ["sh", "-c", "exec java -Dserver.port=$SERVER_PORT -Dserver.address=0.0.0.0 -cp app:app/lib/* prod.individual.sirnilin.ProdIndividualApplication"]