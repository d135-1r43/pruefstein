package com.pruefstein.agent.runner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleStyleTest
{
	/** The CSI prefix every ANSI sequence starts with. */
	private static final String ESCAPE = "\u001B[";

	@AfterEach
	void restoreAnsiDetection()
	{
		System.clearProperty("picocli.ansi");
	}

	@Test
	void paintsPassGreenAndFailRed()
	{
		System.setProperty("picocli.ansi", "true");

		assertTrue(ConsoleStyle.verdict(true).contains("32"), "PASS should carry the green code");
		assertTrue(ConsoleStyle.verdict(false).contains("31"), "FAIL should carry the red code");
		assertTrue(ConsoleStyle.errorTag().contains("31"), "ERROR should carry the red code");
	}

	/**
	 * Green only when the whole run passed: a summary that reads green with a
	 * failure in it is worse than no colour, because it is the line people
	 * trust instead of counting.
	 */
	@Test
	void paintsACleanRunGreen()
	{
		System.setProperty("picocli.ansi", "true");

		assertTrue(ConsoleStyle.summary(5, 5).contains("32"), "a clean run should be green");
	}

	/**
	 * A run with failures stays in the terminal's own foreground colour: the
	 * red belongs on the [FAIL] lines that name the checks, and an explicit
	 * black would disappear on a dark background.
	 */
	@Test
	void leavesAFailedRunUnpainted()
	{
		System.setProperty("picocli.ansi", "true");

		for (String summary : new String[] {ConsoleStyle.summary(4, 5), ConsoleStyle.summary(0, 5)})
		{
			assertTrue(summary.contains("1m"), "the summary should still be bold: " + summary);
			assertFalse(summary.contains("31"), "the summary should not be red: " + summary);
			assertFalse(summary.contains("30"), "the summary should not be an explicit black: " + summary);
			assertFalse(summary.contains("32"), "the summary should not be green: " + summary);
		}
	}

	/**
	 * Red, though the failing summary right above it is not: this is the line
	 * about the deadline, and it says something the [FAIL] lines do not.
	 */
	@Test
	void paintsTheDeadlineNoticeRed()
	{
		System.setProperty("picocli.ansi", "true");

		String notice = ConsoleStyle.notice("2 checks are still failing.");
		assertTrue(notice.contains("31"), "the notice should carry the red code: " + notice);
		assertTrue(notice.contains("1m"), "the notice should be bold too: " + notice);
	}

	/**
	 * The important half: a redirected run — cron mail, a log file, CI — must
	 * come out as plain text, with no escape codes and no leftover markup.
	 */
	@Test
	void writesPlainTextWhereThereIsNoColour()
	{
		System.setProperty("picocli.ansi", "false");

		assertEquals("[PASS]", ConsoleStyle.verdict(true));
		assertEquals("[FAIL]", ConsoleStyle.verdict(false));
		assertEquals("[ERROR]", ConsoleStyle.errorTag());
		assertEquals("Done: 3/5 checks passed", ConsoleStyle.summary(3, 5));
		assertEquals("fix them by Tuesday", ConsoleStyle.notice("fix them by Tuesday"));
		assertEquals("─".repeat(44), ConsoleStyle.rule());
	}

	@Test
	void neverLeaksMarkupOrEscapesIntoTheText()
	{
		System.setProperty("picocli.ansi", "true");

		String summary = ConsoleStyle.summary(3, 5);
		assertTrue(summary.contains(ESCAPE), "colour was forced on, so escapes are expected");
		assertFalse(summary.contains("@|"), "markup should have been consumed");
		assertFalse(summary.contains("|@"), "markup should have been consumed");
		assertTrue(summary.contains("Done: 3/5 checks passed"), "the words have to survive the paint");
	}
}
