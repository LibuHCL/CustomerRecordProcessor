package nl.rabobank.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class RabobankBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(RabobankBackendApplication.class, args);
  }
}
