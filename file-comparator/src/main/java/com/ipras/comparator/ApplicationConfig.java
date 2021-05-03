package com.ipras.comparator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ipras.comparator.compare.ExcelComparator;
import com.ipras.comparator.compare.TextComparator;
import com.ipras.comparator.designer.ReportDesigner;

@Configuration
public class ApplicationConfig {
	
	@Bean
	public ExcelComparator excelComparator() {
		return new ExcelComparator();
	}
	
	@Bean
	public TextComparator textComparator() {
		return new TextComparator();
	}
	
	@Bean
	public ReportDesigner reportDesigner() {
		return new ReportDesigner();
	}
	
}
