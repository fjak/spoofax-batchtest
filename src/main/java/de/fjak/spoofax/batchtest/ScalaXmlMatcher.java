package de.fjak.spoofax.batchtest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class ScalaXmlMatcher {
	private static final Pattern xmlTagEndPattern = compile("(/>)|(</[a-zA-Z:_-]+>)");
	private static final Pattern commentPattern   = compile("^\\s*((\\*)|(/\\*)|(/\\*\\*)|(//))");

	public boolean matches(String string) {
		Matcher xmlTagCloseMatcher = xmlTagEndPattern.matcher(string);
		boolean xmlMatch = xmlTagCloseMatcher.find();
		Matcher commentMatcher = commentPattern.matcher(string);
		boolean commentMatch = commentMatcher.find();
		return xmlMatch && !(commentMatch || string.contains("\"\"\""));
	}
}
