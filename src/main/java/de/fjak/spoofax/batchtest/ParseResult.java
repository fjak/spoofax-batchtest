package de.fjak.spoofax.batchtest;

class ParseResult {
	final ParseResultEnum result;
	final Exception       exception;
	final long            msNeeded;
	final String          path;

	public ParseResult(String path, ParseResultEnum res, long msNeeded) {
		this(path, res, null, msNeeded);
	}

	public ParseResult(String path, ParseResultEnum res, Exception e,
	        long msNeeded) {
		this.path = path;
		this.result = res;
		this.exception = e;
		this.msNeeded = msNeeded;
	}
}