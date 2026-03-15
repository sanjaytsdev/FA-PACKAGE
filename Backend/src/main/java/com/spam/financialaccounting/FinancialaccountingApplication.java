package com.spam.financialaccounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = { 
	DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class})
public class FinancialaccountingApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialaccountingApplication.class, args);
	}

}
