package com.ipras.comparator.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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

public class TextComparator {

	@Autowired
	private ReportDesigner reportDesigner;

	private String sourcePath1;
	private String sourcePath2;

	private String delimiter;
	
	private ReportDesignHandle design;
	private ElementFactory factory;
	int summaryGridRowCount = 1;
	
	public ReportDesignHandle compareText()
			throws BirtException, EncryptedDocumentException, InvalidFormatException, IOException {

		design = reportDesigner.buildReport("plain-text");
		factory = design.getElementFactory();

		// add input parameters to grid
		addInputParameters();

		int[] fileNotFound = { 0, 0 };

		// begin excel comparison

		// load source file
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

		// compare();
		LineIterator leftFile = FileUtils.lineIterator(new File(sourcePath1), "utf-8");
		LineIterator rightFile = FileUtils.lineIterator(new File(sourcePath2), "utf-8");

		int rowNum = 0;

		while (leftFile.hasNext() || rightFile.hasNext()) {

			rowNum++;

			if (leftFile.hasNext() != rightFile.hasNext()) {
				addRowCountMismatch(rowNum);
				break;
			}

			String left = leftFile.nextLine();
			String right = rightFile.nextLine();

			compare(rowNum, Arrays.asList(left.split(",")), Arrays.asList(right.split(",")));
		}

		if (summaryGridRowCount == 1) {
			addNoDiscrepancyFound();
		}

		addStyleSheet();

		return design;
	}

	private void compare(int rowNum, List<String> left, List<String> right) throws IOException, SemanticException {

		if (left.size() != right.size()) {
			addColumnMismatch(rowNum, left.size(), right.size());
			return;
		}

		List<String> leftMismatch = 
		IntStream.range(0, left.size())
		 		 .filter(i -> !left.get(i).equals(right.get(i)))
		 		 .mapToObj(i -> left.get(i))
		 		 .collect(Collectors.toList());
		

		List<String> rightMismatch = 
		IntStream.range(0, right.size())
		 		 .filter(i -> !right.get(i).equals(left.get(i)))
		 		 .mapToObj(i -> right.get(i))
		 		 .collect(Collectors.toList());

		List<Integer> mismatchColumnIndex = IntStream.range(0,left.size())
				 .mapToObj(i -> left.get(i) + ":" + (i+1) + ":" + right.get(i) )
				 .filter(e -> !e.split(":")[0].equals(e.split(":")[2]))
				 .mapToInt(e -> Integer.valueOf(e.split(":")[1]))
				 .mapToObj(e -> e)
				 .collect(Collectors.toList());
		
		if(mismatchColumnIndex.size() == 0) {
			return;
		}
		
		for(int i = 0; i < mismatchColumnIndex.size(); i++) {
			addLineMismatch(rowNum, mismatchColumnIndex.get(i), leftMismatch.get(i), rightMismatch.get(i));
		}
		
	}

	private void addLineMismatch(Integer rowNum, Integer colIndex, String source1Val, String source2Val) throws SemanticException {
		
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);
		
		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Value Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle lineNumText = factory.newTextItem(null);
		lineNumText.setContent(Integer.toString(rowNum));
		cell.getContent().add(lineNumText);
		
		cell = grid.getCell(summaryGridRowCount, 4);
		TextItemHandle colNumText = factory.newTextItem(null);
		colNumText.setContent(Integer.toString(colIndex));
		cell.getContent().add(colNumText);

		cell = grid.getCell(summaryGridRowCount, 5);
		TextItemHandle source1 = factory.newTextItem(null);
		source1.setContent(source1Val);
		cell.getContent().add(source1);

		cell = grid.getCell(summaryGridRowCount, 6);
		TextItemHandle source2 = factory.newTextItem(null);
		source2.setContent(source2Val);
		cell.getContent().add(source2);
		
	}

	private void addColumnMismatch(int leftSize, int rightSize, int rowNum) throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);
		
		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Column Count Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle lineNumText = factory.newTextItem(null);
		lineNumText.setContent(Integer.toString(rowNum));
		cell.getContent().add(lineNumText);

		cell = grid.getCell(summaryGridRowCount, 5);
		TextItemHandle source1 = factory.newTextItem(null);
		source1.setContent(Integer.toString(leftSize));
		cell.getContent().add(source1);

		cell = grid.getCell(summaryGridRowCount, 6);
		TextItemHandle source2 = factory.newTextItem(null);
		source2.setContent(Integer.toString(rightSize));
		cell.getContent().add(source2);

	}

	private void addRowCountMismatch(int rowNum) throws SemanticException {
		summaryGridRowCount++;

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");
		RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
		grid.insertRow(rowParam);

		CellHandle cell = grid.getCell(summaryGridRowCount, 1);
		TextItemHandle serialNo = factory.newTextItem(null);
		serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
		cell.getContent().add(serialNo);
		
		cell = grid.getCell(summaryGridRowCount, 2);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Row Count Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle lineNumText = factory.newTextItem(null);
		lineNumText.setContent(Integer.toString(rowNum));
		cell.getContent().add(lineNumText);

	}

	private void addNoDiscrepancyFound() throws SemanticException {

		GridHandle summaryGrid = (GridHandle) design.findElement("SummaryGrid");
		summaryGrid.drop();
		TextItemHandle text = factory.newTextItem(null);
		text.setProperty("contentType", "HTML");
		text.setContent("<b>No Discrepancy found between the Excel Sources.<b>");

		design.getBody().add(text);
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
				
			}

			for (int i = 1; i <= 6; i++) {
				grid.getCell(1, i).setProperty("style", "header-cell");
			}
		}

	}
	
	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

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

}
