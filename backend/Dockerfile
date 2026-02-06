# ========= Build stage =========
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

#Copia apenas o necessário primeiro
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

#Baixa dependências antes de copiar o código
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests dependency:go-offline

#Copia o restante e compila
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests package

# ========= Runtime stage =========
#Usar alpine para uma imagem menor
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
#Instala curl (para healthcheck do compose)
#apt-get não existe nessa imagem
#RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

#Copia o jar gerado
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
