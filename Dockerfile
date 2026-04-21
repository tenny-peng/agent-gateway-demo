# 1. 使用 Maven 镜像来编译项目
FROM maven:3.6.0-jdk-8-slim as build
WORKDIR /app
# 复制 pom.xml 和源代码
COPY src /app/src
COPY pom.xml /app/
# 编译打包
RUN mvn -f /app/pom.xml clean package

# 2. 使用 OpenJDK 镜像来运行应用
FROM openjdk:8-jre-slim
# 从编译阶段复制打好的 jar 包
COPY --from=build /app/target/*.jar app.jar
# 暴露端口，注意改成你项目里配置的端口
EXPOSE 8080
# 启动应用
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]