package de.fjak.spoofax.batchtest;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.ITreeBuilder;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParseTable;
import org.spoofax.jsglr.client.StartSymbolException;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.binary.TermReader;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Main {
	private static final char C_FAILURE = 'F';
	private static final char C_ERROR = 'E';
	private static final char C_TIMEOUT = 'T';
	private static final char C_AMBIGUITY = 'A';
	private static final char C_SUCCESS = '.';
	private static final char C_STARTMISS = 'S';

	@Parameter(names = { "-s", "--start-symbol" }, description = "Start symbol to use for parsing")
	private String startSymbol = "CompilationUnit";

	@Parameter(names = { "-t", "--timeout" }, description = "Timeout in seconds to parse one file")
	private int timeout = 10;

	@Parameter(names = { "-p", "--parse-table" }, description = "Parse table to use", required = true)
	private String parseTable;

	@Parameter(names = { "--help" }, help = true, hidden = true)
	private boolean help;

	@Parameter
	private List<String> files = new ArrayList<String>();

	private List<String> errorFiles = new LinkedList<String>();
	private List<String> failureFiles = new LinkedList<String>();
	private List<String> ambiguityFiles = new LinkedList<String>();
	private List<String> timeoutFiles = new LinkedList<String>();
	private List<String> startmissFiles = new LinkedList<String>();

	public static void main(String[] args) throws ParseError, IOException,
			InvalidParseTableException {
		Main batchtest = new Main();

		JCommander jCommander = new JCommander(batchtest, args);
		jCommander.setProgramName("batchtest");

		if (batchtest.help) {
			jCommander.usage();
			return;
		}

		batchtest.run();
	}

	private ParseResult parse(SGLR sglr, String filename) {
		try {
			Reader r = new FileReader(filename);
			IStrategoTerm t = (IStrategoTerm) sglr.parse(r, filename, startSymbol);
			TermReader termIO = new TermReader(sglr.getParseTable().getFactory());
			Writer ous = new StringWriter();
			termIO.unparseToFile(t, ous);
			String res = ous.toString();
			if (isAmbiguous(res)) {
				return new ParseResult(ParseResultEnum.AMBIGUITY);
			}
			return new ParseResult(ParseResultEnum.SUCCESS);
		} catch (StartSymbolException e) {
			return new ParseResult(ParseResultEnum.STARTMISS, e);
		} catch (SGLRException e) {
			return new ParseResult(ParseResultEnum.FAILURE, e);
		} catch (Exception e) {
			return new ParseResult(ParseResultEnum.ERROR, e);
		}
	}

	private boolean isAmbiguous(String s) {
		return s.contains("amb(") || s.contains("amb[");
	}

	private SGLR initSGLR(String parseTableFile, String startSymbol)
			throws ParseError, IOException, InvalidParseTableException {
		TermFactory termFactory = new TermFactory();
		IStrategoTerm strategoTerm = new TermReader(termFactory)
				.parseFromFile(parseTableFile);
		ParseTable parseTable = new ParseTable(strategoTerm, termFactory);
		ITreeBuilder treeBuilder = new TreeBuilder(new TermTreeFactory(
				new TermFactory()), true);
		SGLR sglr = new SGLR(treeBuilder, parseTable);
		sglr.getDisambiguator().setFilterAny(true);
		sglr.getDisambiguator().setFilterCycles(true);
		return sglr;
	}

	public void run() throws ParseError, IOException,
			InvalidParseTableException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		SGLR sglr = initSGLR(parseTable, startSymbol);

		for (String file : files) {
			System.out.print(file + ": ");
			try {
				ParseResult res = executor.submit(new TimeoutParse(sglr, file)).get(timeout, TimeUnit.SECONDS);
				handle(res, file);
			} catch (TimeoutException e) {
				timeoutFiles.add(file);
				print(C_TIMEOUT);
				sglr = initSGLR(parseTable, startSymbol);
				executor.shutdownNow();
				executor = Executors.newSingleThreadExecutor();
			} catch (Exception e) {
				errorFiles.add(file);
				print(C_ERROR);
				System.out.println(e.getMessage());
			}
		}
		System.err.println();
		executor.shutdownNow();
		printResults();
	}

	private void handle(ParseResult res, String file) {
		switch (res.result) {
		case AMBIGUITY:
			ambiguityFiles.add(file);
			break;
		case ERROR:
			errorFiles.add(file);
			break;
		case FAILURE:
			failureFiles.add(file);
			break;
		case STARTMISS:
			startmissFiles.add(file);
			break;
		case SUCCESS:
			break;
		}
		print(res.result.signal);
		if (res.exception != null) {
			System.out.println(res.exception.getMessage());
		}
	}

	private void print(char signal) {
		System.err.print(signal);
		System.out.println(signal);
	}

	private void printResults() {
		System.out.println();
		System.out.println(String.format("Finished testing %d files.",
				files.size()));

		printResult(errorFiles, "Errors");
		printResult(failureFiles, "Failures");
		printResult(ambiguityFiles, "Ambiguities");
		printResult(timeoutFiles, "Timeouts");
		printResult(startmissFiles, "Missing Start Symbols");
	}

	private void printResult(List<String> files, String desc) {
		if (!files.isEmpty()) {
			System.out.println();
			System.out.println(String.format("%s (%d):", desc, files.size()));
			for (String file : files) {
				System.out.println(file);
			}
		}
	}

	class TimeoutParse implements Callable<ParseResult> {
		private final SGLR sglr;
		private final String filename;

		public TimeoutParse(SGLR sglr, String filename) {
			this.sglr = sglr;
			this.filename = filename;
		}

		@Override
		public ParseResult call() throws Exception {
			return parse(sglr, filename);
		}
	}

	enum ParseResultEnum {
		SUCCESS(C_SUCCESS), AMBIGUITY(C_AMBIGUITY), FAILURE(C_FAILURE), ERROR(C_ERROR), STARTMISS(C_STARTMISS);

		char signal;

		ParseResultEnum(char signal) {
			this.signal = signal;
		}
	}

	class ParseResult {
		final ParseResultEnum result;
		final Exception exception;

		public ParseResult(ParseResultEnum res) {
			this(res, null);
		}

		public ParseResult(ParseResultEnum res, Exception e) {
			this.result = res;
			this.exception = e;
		}
	}
}
