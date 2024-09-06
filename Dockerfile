# Используем официальный образ OpenJDK в качестве базового образа
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Устанавливаем Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Копируем pom.xml и скачиваем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src /app/src

# Сборка приложения
RUN mvn package -DskipTests

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-crdha5o8fa8c738brvu0-a/weather_app_gem6
ENV SPRING_DATASOURCE_USERNAME=weather_app_gem6_user
ENV SPRING_DATASOURCE_PASSWORD=uzJ6Ge8MSJRsx6uENThPo7hUxXDe5VaR

# Запуск приложения
CMD ["java", "-jar", "target/weather-0.0.1-SNAPSHOT.war"]