package com.pruefstein.agent.runner;

import com.pruefstein.agent.runner.Prompt.Answer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptTest
{
	@Test
	void onlyAnExplicitYesIsAYes()
	{
		assertEquals(Answer.YES, Prompt.interpret("y"));
		assertEquals(Answer.YES, Prompt.interpret("Y"));
		assertEquals(Answer.YES, Prompt.interpret("yes"));
		assertEquals(Answer.YES, Prompt.interpret("  YES  "));
	}

	/**
	 * Both questions the agent asks lead somewhere that is not easily taken
	 * back — a privileged installer, a report filed against someone's machine —
	 * so a bare Enter is a no like any other answer that is not yes.
	 */
	@Test
	void everythingElseIsANo()
	{
		assertEquals(Answer.NO, Prompt.interpret("n"));
		assertEquals(Answer.NO, Prompt.interpret("no"));
		assertEquals(Answer.NO, Prompt.interpret(""));
		assertEquals(Answer.NO, Prompt.interpret("   "));
		assertEquals(Answer.NO, Prompt.interpret("yeah"));
	}

	/**
	 * EOF is not an answer. Cron reaches every prompt with stdin already
	 * closed, and a schedule that quietly reports nothing has to be
	 * distinguishable from someone deciding not to report this run.
	 */
	@Test
	void anAbsentAnswerIsItsOwnCase()
	{
		assertEquals(Answer.NONE, Prompt.interpret(null));
	}
}
