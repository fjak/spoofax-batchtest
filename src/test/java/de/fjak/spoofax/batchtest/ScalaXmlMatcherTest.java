package de.fjak.spoofax.batchtest;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScalaXmlMatcherTest {
	ScalaXmlMatcher uut = new ScalaXmlMatcher();

	@Test
	public void testClosedTag() {
		assertTrue(uut.matches(" as+dhl <yay/>"));
	}

	@Test
	public void testEndTag() {
		assertTrue(uut.matches(" as+dhl </p>  "));
	}

	@Test
	public void testStarClosedTag() {
		assertFalse(uut.matches("   * <yay/>"));
	}

	@Test
	public void testSlashStarClosedTag() {
		assertFalse(uut.matches("   /* <yay/>as"));
	}

	@Test
	public void testSlashStarStarClosedTag() {
		assertFalse(uut.matches("   /** <yay/>asl "));
	}

	@Test
	public void testSlashSlashClosedTag() {
		assertFalse(uut.matches("   // <yay/> asas "));
	}

	@Test
	public void testMultiStringClosedTag() {
		assertFalse(uut.matches("  \"\"\"  <yay/> asas "));
	}

	@Test
	public void testStarEndTag() {
		assertFalse(uut.matches("   * </yay>"));
	}

	@Test
	public void testSlashStarEndTag() {
		assertFalse(uut.matches("   /* </yay>as"));
	}

	@Test
	public void testSlashStarStarEndTag() {
		assertFalse(uut.matches("   /** </yay>asl "));
	}

	@Test
	public void testSlashSlashEndTag() {
		assertFalse(uut.matches("   // </yay> asas "));
	}

	@Test
	public void testMultiStringEndTag() {
		assertFalse(uut.matches("  \"\"\"  </yay> asas "));
	}

}
