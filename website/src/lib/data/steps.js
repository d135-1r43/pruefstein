export const steps = [
	{
		n: '01',
		title: 'Define the control',
		body: 'An admin writes a Compliance Item: a name, an osquery query, and a JEXL expression that must evaluate to true. It sits in a group that maps to an ISO 27001 Annex A control family.',
		meta: 'Web app · admin'
	},
	{
		n: '02',
		title: 'The agent pulls the list',
		body: 'On each laptop, pruefstein-agent fetches the current checks over an OIDC-authenticated call and runs every query through the locally installed osquery.',
		meta: 'GET /api/checks'
	},
	{
		n: '03',
		title: 'Evaluate locally, decide locally',
		body: 'Each expression is evaluated against the full JSON osquery returned. You see pass or fail in your own terminal before anyone else does, and can fix and re-run as often as you like.',
		meta: 'osqueryi --json'
	},
	{
		n: '04',
		title: 'Report, when you say so',
		body: 'Say yes and the run is filed against your account and that hostname. The dashboard updates, the cycle closes, and the outcome mail goes out.',
		meta: 'POST /api/reports'
	}
];
