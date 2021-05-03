package com.ipras.comparator.model;

public class CompareMetadata {

	private String filepath1;
	private String filepath2;
	private String filetype;

	public CompareMetadata(String filepath1, String filepath2, String filetype) {
		super();
		this.filepath1 = filepath1;
		this.filepath2 = filepath2;
		this.filetype = filetype;
	}

	public CompareMetadata() {

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

	@Override
	public String toString() {
		return "CompareMetadata [filepath1=" + filepath1 + ", filepath2=" + filepath2 + ", filetype=" + filetype + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filepath1 == null) ? 0 : filepath1.hashCode());
		result = prime * result + ((filepath2 == null) ? 0 : filepath2.hashCode());
		result = prime * result + ((filetype == null) ? 0 : filetype.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompareMetadata other = (CompareMetadata) obj;
		if (filepath1 == null) {
			if (other.filepath1 != null)
				return false;
		} else if (!filepath1.equals(other.filepath1))
			return false;
		if (filepath2 == null) {
			if (other.filepath2 != null)
				return false;
		} else if (!filepath2.equals(other.filepath2))
			return false;
		if (filetype == null) {
			if (other.filetype != null)
				return false;
		} else if (!filetype.equals(other.filetype))
			return false;
		return true;
	}

}
