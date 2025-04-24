# 第一行必须指定基础容器，我们是java项目，运行需要jdk支撑
FROM eclipse-temurin:17-jdk
# 维护者信息(可选)
MAINTAINER langxi
#   第一个shiro-test.jar为我们项目打出的jar包名称
#   第二个shiro-test.jar为容器名称
ADD RoadCloudVisualizationSystem-0.0.1-SNAPSHOT.jar RoadCloudVisualizationSystem.jar
#   镜像启动容器后暴露的端口
EXPOSE 8080
#   第三个参数RoadCloudVisualizationSystem.jar为容器名称
ENTRYPOINT ["java","-jar","RoadCloudVisualizationSystem.jar"]
