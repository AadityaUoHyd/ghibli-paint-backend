package org.aadi.ghibli_paint;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GhibliPaintBackendApplication {

    static {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            // Make available both ways: system property + env var fallback
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }


    public static void main(String[] args) {
        SpringApplication.run(GhibliPaintBackendApplication.class, args);
    }
}
