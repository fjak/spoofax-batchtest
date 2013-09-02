package de.fjak.spoofax.gt;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import org.spoofax.jsglr.client.ParseException;
import org.spoofax.jsglr.client.ParseTable;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.binary.TermReader;

public class GrammarTest {
	private static String startSymbol = "CompilationUnit";
	private static int timeout = 5;
	private final String parseTable;
	private final List<String> files;

	public static void main(String[] args) throws ParseError, IOException,
			InvalidParseTableException {
		if (args.length < 2) {
			printUsage();
			return;
		}

		String parseTable = args[0];
		List<String> files = new LinkedList<String>();
		for (int i = 1; i < args.length; i++) {
			files.add(args[i]);
		}
		GrammarTest gt = new GrammarTest(parseTable, files);
		gt.run();
	}

	GrammarTest(String parseTable, List<String> files) {
		this.parseTable = parseTable;
		this.files = files;
	}

	private void parse(SGLR sglr, String filename)
			throws TokenExpectedException, BadTokenException, ParseException,
			SGLRException, IOException, InterruptedException,
			AmbiguityException {
		Reader r = new FileReader(filename);
		IStrategoTerm t = (IStrategoTerm) sglr.parse(r, filename, startSymbol);
		TermReader termIO = new TermReader(sglr.getParseTable().getFactory());
		Writer ous = new StringWriter();
		termIO.unparseToFile(t, ous);
		if (ous.toString().contains("amb(") || ous.toString().contains("amb[")) {
			throw new AmbiguityException();
		}
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
		// sglr.getDisambiguator().setAmbiguityIsError(true);
		return sglr;
	}

	private static void printUsage() {
		System.out.println("Usage: de.fjak.spoofax.gt.GrammarTest tbl files...");
	}

	class TimeoutParse implements Callable<Object> {
		private final SGLR sglr;
		private final String filename;

		public TimeoutParse(SGLR sglr, String filename) {
			this.sglr = sglr;
			this.filename = filename;
		}

		@Override
		public Object call() throws Exception {
			parse(sglr, filename);
			return null;
		}
	}

	public void run() throws ParseError, IOException,
			InvalidParseTableException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		SGLR sglr = initSGLR(parseTable, startSymbol);
		List<String> errFiles = new LinkedList<String>();
		List<String> ambFiles = new LinkedList<String>();
		List<String> toutFiles = new LinkedList<String>();
		for (String file : files) {
			System.out.print(file + ": ");
			try {
				executor.submit(new TimeoutParse(sglr, file)).get(timeout, TimeUnit.SECONDS);
				parse(sglr, file);
				System.err.print(".");
				System.out.println(".");
			} catch (AmbiguityException e) {
				ambFiles.add(file);
				System.err.print("A");
				System.out.println("A");
			} catch (TimeoutException e) {
				toutFiles.add(file);
				System.err.print("T");
				System.out.println("T");
				sglr = initSGLR(parseTable, startSymbol);
				executor.shutdownNow();
				executor = Executors.newSingleThreadExecutor();
			} catch (Exception e) {
				errFiles.add(file);
				System.err.print("F");
				System.out.println("F");
			}
		}
		executor.shutdownNow();
		System.err.println();
		System.out.println();
		System.out.println("Errors:");
		for (String errFile : errFiles) {
			System.out.println(errFile);
		}
		System.out.println();
		System.out.println("Ambiguities:");
		for (String ambFile : ambFiles) {
			System.out.println(ambFile);
		}
		System.out.println();
		System.out.println("Timeouts:");
		for (String toutFile : toutFiles) {
			System.out.println(toutFile);
		}
	}
}
