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

# Запуск приложения
CMD ["java", "-jar", "target/weather-application.war"]