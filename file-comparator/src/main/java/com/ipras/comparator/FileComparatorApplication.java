package com.ipras.comparator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FileComparatorApplication {

	private static String[] args;
	private static ConfigurableApplicationContext context;

	public static void main(String[] args) {
		FileComparatorApplication.args = args;
		FileComparatorApplication.context = SpringApplication.run(FileComparatorApplication.class, args);
	}

	public static void restart() {
		// ApplicationArguments args = context.getBean(ApplicationArguments.class);

		context.close();

		// and build new one
		FileComparatorApplication.context = SpringApplication.run(FileComparatorApplication.class, args);
	}

}
