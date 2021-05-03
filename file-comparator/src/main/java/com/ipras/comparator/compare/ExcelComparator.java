package com.ipras.comparator.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.model.api.CellHandle;
import org.eclipse.birt.report.model.api.ElementFactory;
import org.eclipse.birt.report.model.api.GridHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.RowOperationParameters;
import org.eclipse.birt.report.model.api.TextItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.springframework.beans.factory.annotation.Autowired;

import com.ipras.comparator.designer.ReportDesigner;

public class ExcelComparator {

	@Autowired
	private ReportDesigner reportDesigner;

	private String sourcePath1;
	private String sourcePath2;

	public String getSourcePath1() {
		return sourcePath1;
	}

	public void setSourcePath1(String sourcePath1) {
		this.sourcePath1 = sourcePath1;
	}

	public String getSourcePath2() {
		return sourcePath2;
	}

	public void setSourcePath2(String sourcePath2) {
		this.sourcePath2 = sourcePath2;
	}

	private ReportDesignHandle design;
	private ElementFactory factory;
	int summaryGridRowCount = 1;

	public ReportDesignHandle compareExcel()
			throws BirtException, EncryptedDocumentException, InvalidFormatException, IOException {

		design = reportDesigner.buildReport("xlsx");
		factory = design.getElementFactory();

		// add input parameters to grid
		addInputParameters();

		int[] fileNotFound = { 0, 0 };

		// begin excel comparison

		// load souce file
		FileInputStream file1 = null;
		try {
			file1 = new FileInputStream(new File(sourcePath1));
		} catch (FileNotFoundException e) {
			fileNotFound[0] = 1;
		}
		FileInputStream file2 = null;
		try {
			file2 = new FileInputStream(new File(sourcePath2));
		} catch (FileNotFoundException e) {
			fileNotFound[1] = 1;
		}

		if (fileNotFound[0] == 1 || fileNotFound[1] == 1) {
			fileNotFoundMismatch(fileNotFound);
			return design;
		}

		// create workbook objects using source files
		Workbook workbook1 = WorkbookFactory.create(file1);
		Workbook workbook2 = WorkbookFactory.create(file2);

		// compare excels by their sheet name
		// save matched sheet names to common list
		List<String> sheetNames1 = new ArrayList<>();
		List<String> sheetNames2 = new ArrayList<>();
		List<String> matchedSheetNames = new ArrayList<>();

		for (int i = 0; i < workbook1.getNumberOfSheets(); i++) {
			sheetNames1.add(workbook1.getSheetName(i));
		}
		for (int i = 0; i < workbook2.getNumberOfSheets(); i++) {
			sheetNames2.add(workbook2.getSheetName(i));
		}

		ListIterator<String> sheetNames1Iterator = sheetNames1.listIterator();
		while (sheetNames1Iterator.hasNext()) {
			String currentSheet = sheetNames1Iterator.next();

			if (sheetNames2.indexOf(currentSheet) != -1) {
				matchedSheetNames.add(currentSheet);
			} else {
				/**
				 * sheet not found mismatch
				 */
				sheetNames1Iterator.remove();
				addSheetNotFoundException(currentSheet, 1);
			}

		}

		ListIterator<String> sheetNames2Iterator = sheetNames2.listIterator();
		while (sheetNames2Iterator.hasNext()) {
			String currentSheet = sheetNames2Iterator.next();

			if (sheetNames1.indexOf(currentSheet) == -1) {
				/**
				 * sheet not found mismatch
				 */
				sheetNames2Iterator.remove();
				addSheetNotFoundException(currentSheet, 2);
			}

		}

		// further matching the sheets which are in both workbook
		for (int i = 0; i < matchedSheetNames.size(); i++) {
			String sheetName = matchedSheetNames.get(i);
			Sheet sheet1 = workbook1.getSheet(sheetName);
			Sheet sheet2 = workbook2.getSheet(sheetName);

			compare(sheet1, sheet2);
		}

		if (summaryGridRowCount == 1) {
			addNoDiscrepancyFound();
		}

		addStyleSheet();

		return design;
	}

	private void compare(Sheet sheet1, Sheet sheet2) throws SemanticException {

		DataFormatter formatter = new DataFormatter();

		// check if number of rows are equal
		if (sheet1.getPhysicalNumberOfRows() != sheet2.getPhysicalNumberOfRows()) {

			// find which rows are missing
			List<String> row1a = new ArrayList<>();
			for (int i = 1; i < sheet1.getPhysicalNumberOfRows(); i++) {
				Row row = sheet1.getRow(i);
				row1a.add(formatter.formatCellValue(row.getCell(0)));
			}

			List<String> row2a = new ArrayList<>();
			for (int i = 1; i < sheet2.getPhysicalNumberOfRows(); i++) {
				Row row = sheet2.getRow(i);
				row2a.add(formatter.formatCellValue(row.getCell(0)));
			}

			List<String> differences1 = row1a.stream().filter(element -> !row2a.contains(element))
					.collect(Collectors.toList());

			List<String> differences2 = row2a.stream().filter(element -> !row1a.contains(element))
					.collect(Collectors.toList());

			addRowCountMismatch(sheet1.getSheetName(), sheet1.getPhysicalNumberOfRows(),
					sheet2.getPhysicalNumberOfRows(), differences1, differences2);

			return;
		}

		// create data structure to store header metadata
		Map<String, List<String>> columns1 = new HashMap<>();
		Row row = sheet1.getRow(0);

		int firstCellIndex1 = row.getFirstCellNum();
		int lastCellIndex1 = row.getLastCellNum();
		int columnLength1 = lastCellIndex1 - firstCellIndex1;

		for (int i = firstCellIndex1; i < columnLength1; i++) {
			Row keyRow = sheet1.getRow(0);
			String columnKey = formatter.formatCellValue(keyRow.getCell(i));
			List<String> columnValues = new ArrayList<>();
			for (int j = firstCellIndex1 + 1; j < sheet1.getPhysicalNumberOfRows(); j++) {
				Row valueRow = sheet1.getRow(j);
				columnValues.add(formatter.formatCellValue(valueRow.getCell(i)));
			}
			columns1.put(columnKey, columnValues);
		}

		Map<String, List<String>> columns2 = new HashMap<>();
		row = sheet2.getRow(0);

		int firstCellIndex2 = row.getFirstCellNum();
		int lastCellIndex2 = row.getLastCellNum();
		int columnLength2 = lastCellIndex2 - firstCellIndex2;

		for (int i = firstCellIndex2; i < columnLength2; i++) {
			Row keyRow = sheet2.getRow(0);
			String columnKey = formatter.formatCellValue(keyRow.getCell(i));
			List<String> columnValues = new ArrayList<>();
			for (int j = firstCellIndex2 + 1; j < sheet2.getPhysicalNumberOfRows(); j++) {
				Row valueRow = sheet2.getRow(j);
				columnValues.add(formatter.formatCellValue(valueRow.getCell(i)));
			}
			columns2.put(columnKey, columnValues);
		}

		// compare columns in each sheets
		Map<String, List<String>> matchedColumns1 = new HashMap<>();

		Iterator<Entry<String, List<String>>> columns1Iterator = columns1.entrySet().iterator();
		while (columns1Iterator.hasNext()) {
			Entry<String, List<String>> mapElement = columns1Iterator.next();
			String key = mapElement.getKey();

			if (columns2.containsKey(key)) {
				matchedColumns1.put(key, mapElement.getValue());
			} else {
				addMissingColumn(sheet1.getSheetName(), key, 1);
			}
		}

		Map<String, List<String>> matchedColumns2 = new HashMap<>();

		Iterator<Entry<String, List<String>>> columns2Iterator = columns2.entrySet().iterator();
		while (columns2Iterator.hasNext()) {
			Entry<String, List<String>> mapElement = columns2Iterator.next();
			String key = mapElement.getKey();

			if (columns2.containsKey(key)) {
				matchedColumns2.put(key, mapElement.getValue());
			} else {
				addMissingColumn(sheet1.getSheetName(), key, 2);
			}
		}

		// compare data of each cell for matched/same column headers
		Iterator<Entry<String, List<String>>> matchedColumnsIterator = matchedColumns1.entrySet().iterator();
		while (matchedColumnsIterator.hasNext()) {
			Map.Entry<String, List<String>> next = (Map.Entry<String, List<String>>) matchedColumnsIterator.next();

			String key = next.getKey();

			// since key is present in both map
			List<String> values1 = matchedColumns1.get(key);
			List<String> values2 = matchedColumns2.get(key);

			for (int i = 0; i < values1.size(); i++) {
				if (!values1.get(i).equals(values2.get(i))) {
					addValueMismatch(sheet1.getSheetName(), key, (i + 2), values1.get(i), values2.get(i));
				}
			}

		}

	}

	private void addNoDiscrepancyFound() throws SemanticException {

		GridHandle summaryGrid = (GridHandle) design.findElement("SummaryGrid");
		summaryGrid.drop();
		TextItemHandle text = factory.newTextItem(null);
		text.setProperty("contentType", "HTML");
		text.setContent("<b>No Discrepancy found between the Excel Sources.<b>");

		design.getBody().add(text);
	}

	private void addValueMismatch(String sheetName, String column, int i, String value1, String value2)
			throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);

		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle sheetNameText = factory.newTextItem(null);
		sheetNameText.setContent(sheetName);
		cell.getContent().add(sheetNameText);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Value Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 4);
		TextItemHandle columnName = factory.newTextItem(null);
		columnName.setContent(column);
		cell.getContent().add(columnName);

		cell = grid.getCell(summaryGridRowCount, 5);
		TextItemHandle rowNumber = factory.newTextItem(null);
		rowNumber.setContent(Integer.toString(i));
		cell.getContent().add(rowNumber);

		cell = grid.getCell(summaryGridRowCount, 6);
		TextItemHandle source1Text = factory.newTextItem(null);
		source1Text.setContent(value1);
		cell.getContent().add(source1Text);

		cell = grid.getCell(summaryGridRowCount, 7);
		TextItemHandle source2Text = factory.newTextItem(null);
		source2Text.setContent(value2);
		cell.getContent().add(source2Text);

	}

	private void addMissingColumn(String sheetName, String column, int posn) throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);

		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle sheetNameText = factory.newTextItem(null);
		sheetNameText.setContent(sheetName);
		cell.getContent().add(sheetNameText);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Missing Column");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, posn + 5);

		TextItemHandle exception = factory.newTextItem(null);
		exception.setContent(column);
		cell.getContent().add(exception);

	}

	private void addRowCountMismatch(String sheetName, int physicalNumberOfRows1, int physicalNumberOfRows2,
			List<String> missedValues1, List<String> missedValues2) throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);

		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle sheetNameText = factory.newTextItem(null);
		sheetNameText.setContent(sheetName);
		cell.getContent().add(sheetNameText);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Row Count Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 6);

		TextItemHandle exception1 = factory.newTextItem(null);
		exception1.setContent(
				Integer.toString(physicalNumberOfRows1) + " Rows, Missing Keys: " + missedValues2.toString());
		cell.getContent().add(exception1);

		cell = grid.getCell(summaryGridRowCount, 7);

		TextItemHandle exception2 = factory.newTextItem(null);
		exception2.setContent(
				Integer.toString(physicalNumberOfRows2) + " Rows, Missing Keys: " + missedValues1.toString());
		cell.getContent().add(exception2);

	}

	private void fileNotFoundMismatch(int[] fileNotFound) throws SemanticException {

		GridHandle paramGrid = (GridHandle) design.findElement("SummaryGrid");
		paramGrid.drop();
		TextItemHandle text = factory.newTextItem(null);
		text.setProperty("contentType", "HTML");

		if (fileNotFound[0] == 1 && fileNotFound[1] == 1) {
			text.setContent("Source File 1 and 2 was not found on the path specified.");
		} else if (fileNotFound[0] == 1) {
			text.setContent("Source File 1 was not found on the path specified.");
		} else if (fileNotFound[1] == 1) {
			text.setContent("Source File 2 was not found on the path specified.");
		}

		text.setProperty("style", "open-cell");

		design.getBody().add(text);

		addStyleSheet();

	}

	private void addSheetNotFoundException(String currentSheet, int posn) throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);

		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle sheetName = factory.newTextItem(null);
		sheetName.setContent(currentSheet);
		cell.getContent().add(sheetName);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Missing Sheet");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, posn + 5);

		TextItemHandle exception = factory.newTextItem(null);
		exception.setContent(currentSheet);
		cell.getContent().add(exception);

	}

	private void addInputParameters() throws SemanticException {

		GridHandle grid = (GridHandle) design.findElement("ParameterGrid");
		TextItemHandle source1 = factory.newTextItem(null);
		source1.setProperty("contentType", "HTML");
		source1.setContent("Source 1: " + sourcePath1);
		TextItemHandle source2 = factory.newTextItem(null);
		source2.setProperty("contentType", "HTML");
		source2.setContent("Source 2: " + sourcePath2);
		CellHandle cell = grid.getCell(1, 1);
		cell.getContent().add(source1);
		cell = grid.getCell(2, 1);
		cell.getContent().add(source2);

	}

	private void addStyleSheet() throws SemanticException {

		// add style to title
		TextItemHandle title = (TextItemHandle) design.findElement("title");
		title.setProperty("style", "title");

		// add style to parameter grid
		GridHandle grid = (GridHandle) design.findElement("ParameterGrid");
		grid.getCell(1, 1).setProperty("style", "open-cell");
		grid.getCell(2, 1).setProperty("style", "open-cell");

		// add style for run date
		TextItemHandle runDate = (TextItemHandle) design.findElement("runDate");
		runDate.setProperty("style", "open-cell");

		// add style for table title
		TextItemHandle tableTitle = (TextItemHandle) design.findElement("tableTitle");
		tableTitle.setProperty("style", "open-cell");

		if (summaryGridRowCount > 1) {
			// add style to summary grid
			grid = (GridHandle) design.findElement("SummaryGrid");
			for (int i = 1; i <= summaryGridRowCount; i++) {
				grid.getCell(i, 1).setProperty("style", "cell");
				grid.getCell(i, 2).setProperty("style", "cell");
				grid.getCell(i, 3).setProperty("style", "cell");
				grid.getCell(i, 4).setProperty("style", "cell");
				grid.getCell(i, 5).setProperty("style", "cell");
				grid.getCell(i, 6).setProperty("style", "cell");
				grid.getCell(i, 7).setProperty("style", "cell");

			}

			for (int i = 1; i <= 7; i++) {
				grid.getCell(1, i).setProperty("style", "header-cell");
			}
		}

	}
}
