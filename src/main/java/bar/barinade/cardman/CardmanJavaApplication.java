package bar.barinade.cardman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CardmanJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardmanJavaApplication.class, args);
	}

}
