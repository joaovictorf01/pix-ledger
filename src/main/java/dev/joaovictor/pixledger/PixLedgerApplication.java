package dev.joaovictor.pixledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PixLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PixLedgerApplication.class, args);
	}

}
