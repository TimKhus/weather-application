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

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-cql7lijv2p9s739vbi90-a.oregon-postgres.render.com:5432/weather_app_363j
ENV SPRING_DATASOURCE_USERNAME=weather_app_363j_user
ENV SPRING_DATASOURCE_PASSWORD=k1Mk71DxqXHT1ZvXSsIvLjqWd0yFUiqv

# Запуск приложения
CMD ["java", "-jar", "target/weather-0.0.1-SNAPSHOT.war"]