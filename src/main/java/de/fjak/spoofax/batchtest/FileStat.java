package de.fjak.spoofax.batchtest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class FileStat {
	private final int    numLines;
	private final int    numChars;
	private final String path;

	public static FileStat stat(String path) {
		int numLines = 0;
		int numChars = 0;
		String buf;
		try {
			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			while ((buf = br.readLine()) != null) {
				numLines += 1;
				numChars += buf.length();
			}
			fr.close();
		} catch (IOException e) {
			return new FileStat(path, -1, -1);
		}
		return new FileStat(path, numLines, numChars);
	}

	public FileStat(String path, int numLines, int numChars) {
		this.path = path;
		this.numLines = numLines;
		this.numChars = numChars;
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
}