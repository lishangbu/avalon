# 同一个基础镜像用于解包和运行；发布时可通过 Maven 参数固定为 digest。
ARG BASE_IMAGE=public.ecr.aws/amazoncorretto/amazoncorretto:27@sha256:34423799ede3faf8d1a0646d77d4028252e60775a1db087b3871906d815a92b6
FROM ${BASE_IMAGE} AS extract
WORKDIR /build
COPY maven/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --destination /application --application-filename application.jar

FROM ${BASE_IMAGE}
WORKDIR /application
COPY --from=extract /application/ ./
COPY entrypoint.sh /entrypoint.sh
COPY jvm.options /application/jvm.options
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SERVER_PORT=8080 \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
    SPRING_LIQUIBASE_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["sh", "/entrypoint.sh"]
CMD ["plain"]
