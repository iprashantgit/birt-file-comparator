package com.ipras.comparator.model;

public class CompareMetadata {

	private String filepath1;
	private String filepath2;
	private String filetype;
	private String delimiter;
	private String outputType;

	public CompareMetadata() {

	}

	public CompareMetadata(String filepath1, String filepath2, String filetype, String delimiter, String outputType) {
		super();
		this.filepath1 = filepath1;
		this.filepath2 = filepath2;
		this.filetype = filetype;
		this.delimiter = delimiter;
		this.outputType = outputType;
	}

	public String getFilepath1() {
		return filepath1;
	}

	public void setFilepath1(String filepath1) {
		this.filepath1 = filepath1;
	}

	public String getFilepath2() {
		return filepath2;
	}

	public void setFilepath2(String filepath2) {
		this.filepath2 = filepath2;
	}

	public String getFiletype() {
		return filetype;
	}

	public void setFiletype(String filetype) {
		this.filetype = filetype;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

}
