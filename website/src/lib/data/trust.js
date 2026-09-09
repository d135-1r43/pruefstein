/**
 * The trust position. Every item here is a property of the code, not a
 * sentiment; see the agent's Prompt/RunCommand and the report RBAC.
 */
export const trust = [
	{
		title: 'Nothing runs behind your back',
		body: 'No daemon, no kernel extension, no MDM enrolment. The agent starts when somebody starts it, does its work and exits. There is nothing resident on the machine to wonder about.'
	},
	{
		title: 'You see the result first',
		body: 'Queries run on your machine and expressions are evaluated there. Pass and fail land in your own terminal before they land on anybody’s dashboard.'
	},
	{
		title: 'Filing is your yes',
		body: 'Anything short of an explicit “y” is a no, a bare Enter included. Run the checks as often as you like while you fix what they found; nothing is on record until you say so.'
	},
	{
		title: 'The check is readable before it runs',
		body: 'You can read the SQL that will run on your machine and the expression that will judge it. There is no opaque agent behaviour to take on faith and no undisclosed telemetry.'
	},
	{
		title: 'You only see your own',
		body: 'A regular account sees its own reports and nobody else’s. The fleet view belongs to the people who need it for the ISMS, not to everyone with a login.'
	},
	{
		title: 'The deadline is stated, not sprung',
		body: 'The agent prints the date a run would go on record as non-compliant, and a reminder arrives by mail before it does. Nothing about the clock is hidden.'
	}
];
