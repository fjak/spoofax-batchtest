package de.fjak.spoofax.gt;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

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

	public static void main(String[] args) throws ParseError, IOException,
			InvalidParseTableException {
		if (args.length < 2) {
			printUsage();
			return;
		}

		String parseTable = args[0];
		SGLR sglr = initSGLR(parseTable, startSymbol);
		List<String> errFiles = new LinkedList<String>();
		List<String> ambFiles = new LinkedList<String>();
		for (int i = 1; i < args.length; i++) {
			System.out.print(args[i] + ": ");
			try {
				parse(sglr, args[i]);
				System.out.println(".");
				System.err.print(".");
			} catch (SGLRException | InterruptedException e) {
				errFiles.add(args[i]);
				System.out.println("F");
				System.err.print("F");
			} catch (AmbiguityException e) {
				ambFiles.add(args[i]);
				System.out.println("A");
				System.err.print("A");
			}
		}
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
	}

	private static void parse(SGLR sglr, String filename)
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

	private static SGLR initSGLR(String parseTableFile, String startSymbol)
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
}
