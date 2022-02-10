package lastzlo.doctrackerbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocTrackerBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocTrackerBotApplication.class, args);
	}

}
