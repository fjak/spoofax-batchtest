package de.fjak.spoofax.batchtest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class FileStat {
	private final int              numLines;
	private final int              numChars;
	private final String           path;
	private boolean                hasXml;
	private static ScalaXmlMatcher xmlMatcher = new ScalaXmlMatcher();

	public static FileStat stat(String path) {
		int numLines = 0;
		int numChars = 0;
		boolean hasXml = false;
		String buf;
		try {
			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			while ((buf = br.readLine()) != null) {
				numLines += 1;
				numChars += buf.length();
				if (xmlMatcher.matches(buf)) {
					hasXml = true;
				}
			}
			fr.close();
		} catch (IOException e) {
			return new FileStat(path, -1, -1, false);
		}
		return new FileStat(path, numLines, numChars, hasXml);
	}

	public FileStat(String path, int numLines, int numChars, boolean hasXml) {
		this.path = path;
		this.numLines = numLines;
		this.numChars = numChars;
		this.hasXml = hasXml;
	}

	public int getNumLines() {
		return numLines;
	}

	public int getNumChars() {
		return numChars;
	}

	public String getPath() {
		return path;
	}

	public boolean hasXml() {
		return hasXml;
	}
}