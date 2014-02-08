package de.fjak.spoofax.batchtest;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import static java.lang.System.out;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.String.format;

import static de.fjak.spoofax.batchtest.ParseResultEnum.*;

public class Main {
	@Parameter(names = { "-s", "--start-symbol" },
	        description = "Start symbol to use for parsing")
	private String                startSymbol    = "CompilationUnit";

	@Parameter(names = { "-t", "--timeout" },
	        description = "Timeout in seconds to parse one file")
	private int                   timeout        = 10;

	@Parameter(names = { "-p", "--parse-table" },
	        description = "Parse table to use", required = true)
	private String                parseTable;

	@Parameter(names = { "--help" }, help = true, hidden = true)
	private boolean               help;

	@Parameter
	private List<String>          files          = new ArrayList<String>();

	private List<String>          errorFiles     = new LinkedList<String>();
	private List<String>          failureFiles   = new LinkedList<String>();
	private List<String>          ambiguityFiles = new LinkedList<String>();
	private List<String>          timeoutFiles   = new LinkedList<String>();
	private List<String>          startmissFiles = new LinkedList<String>();
	private Map<String, FileStat> fileStats      = new HashMap<String, FileStat>();

	/**
	 * Parse time for the input files in milliseconds
	 */
	private Map<String, Long>     timeParses     = new HashMap<String, Long>();
	private long                  msInit         = 0L;
	private Stopwatch             stopwatch      = new Stopwatch();

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

	private ParseResult parse(SGLR sglr, String path) {
		stopwatch.reset().start();
		try {
			Reader r = new FileReader(path);
			IStrategoTerm t = (IStrategoTerm) sglr.parse(r, path, startSymbol);
			TermReader termIO = new TermReader(sglr.getParseTable()
			        .getFactory());
			Writer ous = new StringWriter();
			termIO.unparseToFile(t, ous);
			String res = ous.toString();
			long msNeeded = stopwatch.read();
			if (isAmbiguous(res)) {
				return new ParseResult(path, AMBIGUITY, msNeeded);
			}
			return new ParseResult(path, SUCCESS, msNeeded);
		} catch (StartSymbolException e) {
			return new ParseResult(path, STARTMISS, e, stopwatch.read());
		} catch (SGLRException e) {
			return new ParseResult(path, FAILURE, e, stopwatch.read());
		} catch (Exception e) {
			return new ParseResult(path, ERROR, e, stopwatch.read());
		} finally {
			stopwatch.stop();
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
		TermTreeFactory ttfac = new TermTreeFactory(new TermFactory());
		ITreeBuilder treeBuilder = new TreeBuilder(ttfac, true);
		SGLR sglr = new SGLR(treeBuilder, parseTable);
		sglr.getDisambiguator().setFilterAny(true);
		sglr.getDisambiguator().setFilterCycles(true);
		return sglr;
	}

	public void run() throws ParseError, IOException,
	        InvalidParseTableException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		stopwatch.reset().start();
		SGLR sglr = initSGLR(parseTable, startSymbol);
		msInit = stopwatch.stop().read();

		String head = format("RX, %7s, %4s, %6s: %s", "time", "loc", "chars",
		        "path");
		out.println(head);
		for (String path : files) {
			try {
				TimeoutParse parse = new TimeoutParse(sglr, path);
				ParseResult res = executor.submit(parse).get(timeout,
				        TimeUnit.SECONDS);
				handle(res);
			} catch (TimeoutException e) {
				handle(new ParseResult(path, TIMEOUT, timeout * 1000L));
				sglr = initSGLR(parseTable, startSymbol);
				executor.shutdownNow();
				executor = Executors.newSingleThreadExecutor();
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}
		}
		err.println();
		executor.shutdownNow();
		printResults();
	}

	private void handle(ParseResult res) {
		String path = res.path;
		switch (res.result) {
		case AMBIGUITY:
			ambiguityFiles.add(path);
			break;
		case ERROR:
			errorFiles.add(path);
			break;
		case FAILURE:
			failureFiles.add(path);
			break;
		case STARTMISS:
			startmissFiles.add(path);
			break;
		case TIMEOUT:
			timeoutFiles.add(path);
			break;
		case SUCCESS:
			break;
		}
		timeParses.put(path, res.msNeeded);
		fileStats.put(path, FileStat.stat(path));
		printErr(res);
		printIntermediate(res);
		if (res.exception != null) {
			out.println(res.exception.getMessage());
		}
	}

	private void printIntermediate(ParseResult res) {
		String path   = res.path;
		FileStat stat = fileStats.get(path);
		char signal   = res.result.signal;
		double time   = res.msNeeded / 1000.0;
		int nLoc      = stat.getNumLines();
		int nChars    = stat.getNumChars();
		char xml      = ' ';
		if (stat.hasXml()) {
			xml = 'X';
		}
		String s = format("%c%c, %6.3fs, %4d, %6d: %s", signal, xml, time, nLoc,
		        nChars, path);
		out.println(s);
	}

	private void printErr(ParseResult res) {
		err.print(res.result.signal);
	}

	private void printResults() {
		out.println();
		out.println(format("Finished testing %d files.", files.size()));
		out.println(format("Initial parser setup took %2.3fs.", msInit / 1000.0));
		printResult(errorFiles, "Errors");
		printResult(failureFiles, "Failures");
		printResult(ambiguityFiles, "Ambiguities");
		printResult(timeoutFiles, "Timeouts");
		printResult(startmissFiles, "Missing Start Symbols");
	}

	private void printResult(List<String> files, String desc) {
		if (!files.isEmpty()) {
			out.println();
			out.println(format("%s (%d):", desc, files.size()));
			for (String file : files) {
				out.println(file);
			}
		}
	}

	class TimeoutParse implements Callable<ParseResult> {
		private final SGLR   sglr;
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
}
