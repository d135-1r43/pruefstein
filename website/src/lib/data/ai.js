/**
 * The two AI jobs. Wording follows what the shipped system prompts actually
 * ask for; see web/src/main/resources/prompts/.
 */
export const aiJobs = [
	{
		tag: 'Create',
		title: 'Describe the control, get the check',
		body: 'Write what you want in plain English. Back comes a name, an osquery query and the JEXL expression that judges it, generated against the osquery schema that ships with the app rather than from memory.',
		points: [
			'For a blocked application you get the bundle identifiers, Homebrew formula and cask names and .app filenames that recognise it however it arrived.',
			'It writes the policy reason too, which is the line your A.12.6.2 audit trail needs.',
			'The prompt is told to return an empty list rather than a plausible guess. An invented bundle id silently matches nothing and looks like coverage.'
		]
	},
	{
		tag: 'Fix',
		title: 'Explain the failure in plain words',
		body: 'A failed check gets a short headline and a step-by-step remedy written against the osquery output the machine actually returned, not against the check’s description.',
		points: [
			'A blacklisted app gets the exact removal step for how it was really installed: brew uninstall --cask, or the bundle path to move to the Trash.',
			'Installed by both routes? It is told to give both steps, because removing one leaves the other behind.',
			'Explanations are written on a schedule outside the report transaction, so a slow model never holds up an upload.'
		]
	}
];
