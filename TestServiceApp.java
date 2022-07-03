package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class TestServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(TestServiceApp.class, args);
	}

	@EventListener
	public void onApplicationEvent(ServletWebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		System.out.println("TESTNG-SERVICE-PORT="+port);
	}
}
