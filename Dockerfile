# Paso 1: Elegir una imagen base.
# Usamos una imagen oficial de OpenJDK con una distribución Linux ligera (Alpine o Slim).
# Esto reduce el tamaño de la imagen final del contenedor.
# Asegúrate de que la versión de JDK (ej. 17, 21) coincida con la que usas en tu proyecto.
FROM openjdk:17-jdk-slim

# Paso 2: Establecer los metadatos de la imagen (opcional, pero buena práctica).
LABEL maintainer="m.valera@arrabalempleo.orf"
LABEL description="Backend de Spring Boot para YBS IA"

# Instalar librerías necesarias para JasperReports en un entorno headless
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libfontconfig1 \
    libfreetype6 \
    # Estas pueden ser necesarias para ciertos tipos de imágenes en los reportes:
    # libjpeg-turbo8 \
    # libpng16-16 \
    # libgif7 \
    && rm -rf /var/lib/apt/lists/*

# Paso 3: Establecer el directorio de trabajo dentro del contenedor.
# Aquí es donde se copiará tu JAR y se ejecutará el comando.
WORKDIR /app

# Paso 4: Copiar el archivo JAR ejecutable a la imagen.
# Asume que tu JAR se encuentra en 'target/tu-aplicacion-0.0.1-SNAPSHOT.jar'
# Cambia 'tu-aplicacion-0.0.1-SNAPSHOT.jar' por el nombre real de tu archivo JAR.
# Lo copiamos como 'app.jar' dentro del contenedor para simplificar el comando de ejecución.
COPY target/incide-0.0.1-SNAPSHOT.jar app.jar

# Paso 5: Exponer el puerto en el que tu aplicación Spring Boot escucha.
# Por defecto, Spring Boot escucha en el puerto 8080.
EXPOSE 8080

# Paso 6: Definir el comando para ejecutar la aplicación cuando el contenedor se inicie.
# Usa 'java -jar' para ejecutar el JAR ejecutable.
# Puedes añadir opciones de JVM aquí si las necesitas (ej., -Xmx512m).
ENTRYPOINT ["java", "-jar", "app.jar"]