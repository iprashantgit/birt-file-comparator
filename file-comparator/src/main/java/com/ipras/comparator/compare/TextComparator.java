package com.ipras.comparator.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.text.diff.StringsComparator;
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

	private ReportDesignHandle design;
	private ElementFactory factory;
	int summaryGridRowCount = 1;

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

		compare();

		if (summaryGridRowCount == 1) {
			addNoDiscrepancyFound();
		}

		addStyleSheet();

		return design;
	}

	private void compare() throws IOException, SemanticException {

		// Read both files with line iterator.
		LineIterator file1 = FileUtils.lineIterator(new File(sourcePath1), "utf-8");
		LineIterator file2 = FileUtils.lineIterator(new File(sourcePath2), "utf-8");

		int lineNum = 0;

		// Initialize visitor.
		FileCommandsVisitor fileCommandsVisitor = new FileCommandsVisitor();

		// Read file line by line so that comparison can be done line by line.
		while (file1.hasNext() || file2.hasNext()) {
			/*
			 * In case both files have different number of lines, fill in with empty
			 * strings. Also append newline char at end so next line comparison moves to
			 * next line.
			 */
			lineNum++;
			fileCommandsVisitor = new FileCommandsVisitor();

			String left = (file1.hasNext() ? file1.nextLine() : "") + "\n";
			String right = (file2.hasNext() ? file2.nextLine() : "") + "\n";

			// Prepare diff comparator with lines from both files.
			StringsComparator comparator = new StringsComparator(left, right);

			if (comparator.getScript().getLCSLength() > (Integer.max(left.length(), right.length()) * 0.4)) {
				/*
				 * If both lines have atleast 40% commonality then only compare with each other
				 * so that they are aligned with each other in final diff HTML.
				 */

				comparator.getScript().visit(fileCommandsVisitor);
				addLineMismatch(lineNum, fileCommandsVisitor.left, fileCommandsVisitor.right);
			} else {
				/*
				 * If both lines do not have 40% commanlity then compare each with empty line so
				 * that they are not aligned to each other in final diff instead they show up on
				 * separate lines.
				 */
				StringsComparator leftComparator = new StringsComparator(left, "\n");
				leftComparator.getScript().visit(fileCommandsVisitor);
				StringsComparator rightComparator = new StringsComparator("\n", right);
				rightComparator.getScript().visit(fileCommandsVisitor);
				addLineMismatch(lineNum, fileCommandsVisitor.left, fileCommandsVisitor.right);
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

	private void addLineMismatch(int lineNum, String left, String right) throws SemanticException {

		GridHandle grid = (GridHandle) design.findElement("SummaryGrid");

		// create text for left

		int leftTextHeight = 0;

		if (left.indexOf('<') == -1) {

			summaryGridRowCount++;

			RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
			grid.insertRow(rowParam);

			CellHandle cell = grid.getCell(summaryGridRowCount, 1);
			TextItemHandle serialNo = factory.newTextItem(null);
			serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
			cell.getContent().add(serialNo);

			cell = grid.getCell(summaryGridRowCount, 2);
			TextItemHandle lineNumText = factory.newTextItem(null);
			lineNumText.setContent("Line Number " + lineNum);
			cell.getContent().add(lineNumText);

			cell = grid.getCell(summaryGridRowCount, 3);
			TextItemHandle mismatchType = factory.newTextItem(null);
			mismatchType.setContent("Value Mismatch");
			cell.getContent().add(mismatchType);

			cell = grid.getCell(summaryGridRowCount, 4);
			TextItemHandle leftText = factory.newTextItem(null);
			leftText.setContent(left);
			cell.getContent().add(leftText);

		} else {

			int beginIndex = 0;

			while (left.length() > 0) {

				if (left.indexOf('<') == -1) {

					summaryGridRowCount++;
					leftTextHeight++;

					System.out.println(left);

					RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
					grid.insertRow(rowParam);

					CellHandle cell = grid.getCell(summaryGridRowCount, 4);

					TextItemHandle leftText = factory.newTextItem(null);
					leftText.setContent(left);
					cell.getContent().add(leftText);

					break;

				} else {

					int index = left.indexOf('<');

					System.out.println(beginIndex + "---" + index);
					System.out.println(left);

					summaryGridRowCount++;
					leftTextHeight++;

					RowOperationParameters rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
					grid.insertRow(rowParam);

					if (leftTextHeight == 1) {
						CellHandle cell = grid.getCell(summaryGridRowCount, 1);
						TextItemHandle serialNo = factory.newTextItem(null);
						serialNo.setContent(Integer.toString(summaryGridRowCount - 1));
						cell.getContent().add(serialNo);

						cell = grid.getCell(summaryGridRowCount, 2);
						TextItemHandle lineNumText = factory.newTextItem(null);
						lineNumText.setContent("Line Number " + lineNum);
						cell.getContent().add(lineNumText);

						cell = grid.getCell(summaryGridRowCount, 3);
						TextItemHandle mismatchType = factory.newTextItem(null);
						mismatchType.setContent("Value Mismatch");
						cell.getContent().add(mismatchType);
					}

					CellHandle cell = grid.getCell(summaryGridRowCount, 4);

					TextItemHandle leftText = factory.newTextItem(null);
					leftText.setContent(left.substring(beginIndex, index));
					cell.getContent().add(leftText);

					summaryGridRowCount++;
					leftTextHeight++;

					rowParam = new RowOperationParameters(1, 0, summaryGridRowCount - 1);
					grid.insertRow(rowParam);

					cell = grid.getCell(summaryGridRowCount, 4);
					cell.setOnRender("this.getStyle().backgroundColor=\"green\";");

					leftText = factory.newTextItem(null);
					leftText.setContent(left.substring(index + 1, index + 2));
					cell.getContent().add(leftText);

					beginIndex = index + 2;
					left = left.substring(beginIndex, left.length());
				}

			}
		}

		// create text for right
		int rightTextHeight = 0;

		if (right.indexOf('<') == -1) {

			CellHandle cell = grid.getCell(summaryGridRowCount, 5);

			TextItemHandle rightText = factory.newTextItem(null);
			rightText.setContent(right);
			cell.getContent().add(rightText);

		} else {
			int beginIndex = 0;

			while (right.length() > 0) {

				if (right.indexOf('<') == -1) {

					rightTextHeight++;
					CellHandle cell = grid.getCell(summaryGridRowCount - (leftTextHeight - rightTextHeight), 5);

					TextItemHandle rightText = factory.newTextItem(null);
					rightText.setContent(right);
					cell.getContent().add(rightText);

					break;

				} else {

					int index = right.indexOf('<');

					System.out.println(beginIndex + "---" + index);
					System.out.println(right);

					TextItemHandle rightText = factory.newTextItem(null);
					rightText.setContent(right.substring(beginIndex, index));
					rightTextHeight++;
					CellHandle cell = grid.getCell(summaryGridRowCount - (leftTextHeight - rightTextHeight), 5);
					cell.getContent().add(rightText);

					rightTextHeight++;
					cell = grid.getCell(summaryGridRowCount - (leftTextHeight - rightTextHeight), 5);
					cell.setOnRender("this.getStyle().backgroundColor=\"red\";");

					rightText = factory.newTextItem(null);
					rightText.setContent(right.substring(index + 1, index + 2));

					cell.getContent().add(rightText);

					beginIndex = index + 2;
					right = right.substring(beginIndex, right.length());
				}
			}
		}

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

			}

			for (int i = 1; i <= 5; i++) {
				grid.getCell(1, i).setProperty("style", "header-cell");
			}
		}

	}

}
