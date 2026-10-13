# 同一个基础镜像用于解包和运行；发布时可通过 Maven 参数固定为 digest。
ARG BASE_IMAGE=docker.io/library/amazoncorretto:27@sha256:3bca6d83fb33e63fe5386c7f8e290e4f70ecb2a406ae59eaf5cfd6fddca01563
FROM ${BASE_IMAGE} AS extract
WORKDIR /build
COPY maven/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination /application --application-filename application.jar

FROM ${BASE_IMAGE}
WORKDIR /application
# 按变更频率分层复制，业务代码更新时复用依赖层。
COPY --from=extract /application/dependencies/ ./
COPY --from=extract /application/spring-boot-loader/ ./
COPY --from=extract /application/snapshot-dependencies/ ./
COPY --from=extract /application/application/ ./
COPY jvm.options /application/jvm.options
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SERVER_PORT=8080 \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
    SPRING_LIQUIBASE_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["java", "@jvm.options", "-jar", "application.jar"]
