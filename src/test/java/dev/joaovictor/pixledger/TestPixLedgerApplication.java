package dev.joaovictor.pixledger;

import org.springframework.boot.SpringApplication;

public class TestPixLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.from(PixLedgerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
