package com.ipras.comparator.compare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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

public class TextComparator{

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

		
		compare(file1, file2);


		if (summaryGridRowCount == 1) {
			addNoDiscrepancyFound();
		}

		addStyleSheet();

		return design;
	}
	
	private void compare(FileInputStream file1, FileInputStream file2) throws SemanticException, IOException {

		// new code
		BufferedReader reader1 = new BufferedReader(new FileReader(sourcePath1));
        
        BufferedReader reader2 = new BufferedReader(new FileReader(sourcePath2));
         
        String line1 = reader1.readLine();
         
        String line2 = reader2.readLine();
         
        boolean areEqual = true;
         
        int lineNum = 1;
         
        while (line1 != null || line2 != null)
        {
            if(line1 == null || line2 == null)
            {
                areEqual = false;
                 
                break;
            }
            else if(! line1.equalsIgnoreCase(line2))
            {
                areEqual = false;
                
                addLineMismatch(lineNum, line1, line2);
                
                break;
            }
             
            line1 = reader1.readLine();
             
            line2 = reader2.readLine();
             
            lineNum++;
        }
         
        if(areEqual)
        {
            System.out.println("Two files have same content.");
        }
        else
        {
            System.out.println("Two files have different content. They differ at line "+lineNum);
             
            System.out.println("File1 has "+line1+" and File2 has "+line2+" at line "+lineNum);
        }
         
        reader1.close();
         
        reader2.close();
	}

	private void addNoDiscrepancyFound() throws SemanticException {

		GridHandle summaryGrid = (GridHandle) design.findElement("SummaryGrid");
		summaryGrid.drop();
		TextItemHandle text = factory.newTextItem(null);
		text.setProperty("contentType", "HTML");
		text.setContent("<b>No Discrepancy found between the Excel Sources.<b>");

		design.getBody().add(text);
	}

	
	private void addLineMismatch(int lineNum, String line1, String line2)
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
		TextItemHandle lineNumText = factory.newTextItem(null);
		lineNumText.setContent("Line Number " + lineNum);
		cell.getContent().add(lineNumText);

		cell = grid.getCell(summaryGridRowCount, 3);
		TextItemHandle mismatchType = factory.newTextItem(null);
		mismatchType.setContent("Value Mismatch");
		cell.getContent().add(mismatchType);

		cell = grid.getCell(summaryGridRowCount, 4);
		TextItemHandle line1Text = factory.newTextItem(null);
		line1Text.setContent(line1);
		cell.getContent().add(line1Text);

		cell = grid.getCell(summaryGridRowCount, 5);
		TextItemHandle line2Text = factory.newTextItem(null);
		line2Text.setContent(line2);
		cell.getContent().add(line2Text);

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
