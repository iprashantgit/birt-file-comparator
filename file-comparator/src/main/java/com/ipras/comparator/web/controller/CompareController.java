package com.ipras.comparator.web.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ipras.comparator.ApplicationConfig;
import com.ipras.comparator.FileComparatorApplication;
import com.ipras.comparator.birt.service.BirtReportService;
import com.ipras.comparator.compare.ExcelComparator;
import com.ipras.comparator.compare.TextComparator;
import com.ipras.comparator.model.CompareMetadata;

@Controller
public class CompareController {

	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfig.class);

	@Autowired
	private BirtReportService reportService;

	public ReportDesignHandle design;

	ExcelComparator excelComparator = context.getBean(ExcelComparator.class);
	TextComparator textComparator = context.getBean(TextComparator.class);

	String outputType = "xlsx";

	@GetMapping(value = "/compare")
	public String compare(Model model) {
		CompareMetadata compareMetadata = new CompareMetadata();
		model.addAttribute("compareMetadata", compareMetadata);

		List<String> listFileTypes = Arrays.asList("xlsx", "plain-text");
		model.addAttribute("listFileTypes", listFileTypes);

		return "compare_form";
	}

	@PostMapping(value = "/compare")
	public String compare(@ModelAttribute("compareMetadata") CompareMetadata compareMetadata)
			throws EncryptedDocumentException, InvalidFormatException, BirtException, IOException {

		if (compareMetadata.getFiletype().equals("xlsx")) {
			excelComparator.setSourcePath1(compareMetadata.getFilepath1());
			excelComparator.setSourcePath2(compareMetadata.getFilepath2());
			design = excelComparator.compareExcel();
		}

		if (compareMetadata.getFiletype().equals("plain-text")) {
			textComparator.setSourcePath1(compareMetadata.getFilepath1());
			textComparator.setSourcePath2(compareMetadata.getFilepath2());
			design = textComparator.compareText();
		}

		return "compare_result";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/result")
	@ResponseBody
	public void generateFullReport(HttpServletResponse response, HttpServletRequest request)
			throws EncryptedDocumentException, InvalidFormatException, BirtException, IOException {

		reportService.generateMainReport(design, response, request, outputType);

	}

	@GetMapping("/restart")
	public String restart() {
		Thread restartThread = new Thread(() -> {
			try {
				Thread.sleep(1000);
				FileComparatorApplication.restart();
			} catch (InterruptedException ignored) {
			}
		});
		restartThread.setDaemon(false);
		restartThread.start();
		// FileComparatorApplication.restart();
		// return "/";
		return "restart_success";
	}

}
