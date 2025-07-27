package com.encuestas.incide;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class IncideApplication {

	public static void main(String[] args) {
		// --- Cargar el archivo .env aquí ---
		try {
			// Carga el .env desde el directorio raíz de tu proyecto.
			// Las variables se añadirán a las propiedades del sistema.
			Dotenv dotenv = Dotenv.load();
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
			System.out.println(".env file loaded successfully."); // Opcional: para depuración
		} catch (io.github.cdimascio.dotenv.DotenvException e) {
			// Manejar la excepción si el .env no se encuentra o hay un problema.
			// Para entornos de producción, es posible que las variables ya estén establecidas.
			System.err.println("Warning: .env file not found or could not be loaded. Ensure variables are set in environment. " + e.getMessage());
		}
		// --- Fin de carga .env ---
		SpringApplication.run(IncideApplication.class, args);
	}

}
