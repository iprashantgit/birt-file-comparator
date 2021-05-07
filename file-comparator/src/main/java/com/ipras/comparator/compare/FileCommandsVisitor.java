package com.ipras.comparator.compare;

import java.io.IOException;

import org.apache.commons.text.diff.CommandVisitor;

public class FileCommandsVisitor implements CommandVisitor<Character> {

	// Spans with red & green highlights to put highlighted characters in HTML
	//private static final String DELETION = "<span style=\"background-color: #FB504B\">${text}</span>";
	//private static final String INSERTION = "<span style=\"background-color: #45EA85\">${text}</span>";

	private static final String DELETION = "<${text}";
	private static final String INSERTION = "<${text}";

	
	public String left = "";
	public String right = "";

	@Override
	public void visitKeepCommand(Character c) {
		// For new line use <br/> so that in HTML also it shows on next line.
		String toAppend = "\n".equals("" + c) ? "" : "" + c;
		// KeepCommand means c present in both left & right. So add this to both without
		// any
		// highlight.
		left = left + toAppend;
		right = right + toAppend;
	}

	@Override
	public void visitInsertCommand(Character c) {
		// For new line use <br/> so that in HTML also it shows on next line.
		String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
		// InsertCommand means character is present in right file but not in left. Show
		// with green highlight on right.
		right = right + INSERTION.replace("${text}", "" + toAppend);
	}

	@Override
	public void visitDeleteCommand(Character c) {
		// For new line use <br/> so that in HTML also it shows on next line.
		String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
		// DeleteCommand means character is present in left file but not in right. Show
		// with red highlight on left.
		left = left + DELETION.replace("${text}", "" + toAppend);
	}

	public String generateHTML() throws IOException {

		// Get template & replace placeholders with left & right variables with actual
		// comparison
		String template = "<div style=\"height: 100%; width: 50%; position: fixed; z-index: 1; overflow-x: hidden; padding-top: 20px; left: 0; background-color: #D7FBF6;\"> <div> <p>${left}</p> </div> </div> <div style=\"height: 100%; width: 50%; position: fixed; z-index: 1; overflow-x: hidden; padding-top: 20px; right: 0; background-color: #FCFCC3;\"> <div> <p>${right}</p> </div> </div>";
		String out1 = template.replace("${left}", left);
		String output = out1.replace("${right}", right);
		return output;
	}
}
