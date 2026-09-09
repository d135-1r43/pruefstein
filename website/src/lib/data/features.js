/**
 * The feature grid. `tag` renders in the app's tiny all-caps label style;
 * `accent: true` flips a card to the yellow accent so the grid has rhythm.
 */
export const features = [
	{
		tag: 'Checks',
		title: 'A control is a query, not a checkbox',
		body: 'Every check is osquery SQL plus one JEXL expression over the JSON result. Anybody who reads SQL can audit what the control actually asserts. Your auditor included.',
		accent: true
	},
	{
		tag: 'Catalog',
		title: '19 macOS checks on first boot',
		body: 'FileVault, firewall, Gatekeeper, SIP, screen lock, automatic updates, remote login, sharing services. Seeded against permanent keys, so upgrades never duplicate or overwrite what you have edited. Windows and Linux are not covered yet.'
	},
	{
		tag: 'Blocked apps',
		title: 'One rule per application',
		body: 'An app arrives as a Homebrew cask and an .app bundle; one rule carries every matcher it needs (bundle id, app name, Homebrew formula), so the admin sees one row and one reason rather than four patterns.'
	},
	{
		tag: 'Agent',
		title: 'One small binary per machine',
		body: 'A Java CLI that builds to a native image and starts in about 20 ms. It runs osqueryi, evaluates the expressions locally and posts the result. No daemon, no kernel extension, no MDM.'
	},
	{
		tag: 'Consent',
		title: 'Checking is free. Reporting is deliberate.',
		body: 'Running the checks tells you where you stand and costs nothing. Filing the report is a separate, explicit yes, so a laptop can be checked and fixed as often as it takes before anything lands on a dashboard.',
		accent: true
	},
	{
		tag: 'Identity',
		title: 'Device-flow OIDC, no agent config',
		body: 'The agent carries no identity configuration. It asks the server for the issuer, client id and scopes, then discovers the endpoints itself. The same binary works against Keycloak and Microsoft Entra ID without a rebuild.'
	},
	{
		tag: 'Dashboard',
		title: 'The fleet at a glance',
		body: 'Compliant, non-compliant, missing and pending, counted and filterable. Drill into any run to see the raw osquery JSON that produced the verdict.'
	},
	{
		tag: 'Cycles',
		title: 'Deadlines that enforce themselves',
		body: 'One reporting workflow runs per device at all times. Report inside the window and the cycle closes; miss it and a MISSING report is filed automatically and the next cycle starts.'
	},
	{
		tag: 'Mail',
		title: 'Told, not surprised',
		body: 'People get the outcome of their own run and a reminder before a deadline. Sending is fire-and-forget by design: an SMTP server that is down must never fail a report upload.'
	},
	{
		tag: 'Inventory',
		title: 'It knows what is installed',
		body: 'Each run also takes stock of the machine: application bundles a person installed, plus Homebrew formulae and casks. That gives a blocked-app rule something to match, and answers "who still has that?" without anybody being asked.'
	},
	{
		tag: 'Security',
		title: 'Deny by default',
		body: 'Unannotated endpoints are refused at build time, not at review time. Two roles: people see their own reports, admins see the fleet and hold the CRUD.'
	},
	{
		tag: 'Devices',
		title: 'People have more than one laptop',
		body: 'One account, many machines. Each device reports independently under its own hostname and carries its own compliance state.'
	}
];
