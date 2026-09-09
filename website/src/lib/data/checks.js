/**
 * Real checks, copied verbatim from the seeded catalog
 * (web/.../compliance/bootstrap/ComplianceCatalog.java). If the catalog
 * changes, change these too. The whole point is that they are not mockups.
 */
export const examples = [
	{
		id: 'filevault',
		label: 'Disk encryption',
		group: 'A.10 Cryptography',
		name: 'FileVault enabled',
		sql: "SELECT filevault_status\n  FROM disk_encryption\n WHERE filevault_status = 'on'\n LIMIT 1;",
		jexl: 'results.size() > 0',
		reads: 'Passes when a row comes back at all. The volume is encrypted.',
		json: '[\n  { "filevault_status": "on" }\n]',
		passed: true
	},
	{
		id: 'firewall',
		label: 'Firewall',
		group: 'A.12 Operations Security',
		name: 'Firewall enabled',
		sql: 'SELECT global_state FROM alf;',
		jexl: "results.size() > 0 && results[0].global_state == '1'",
		reads: 'Passes only when the application firewall reports state 1. This machine returned 0, so it fails.',
		json: '[\n  { "global_state": "0" }\n]',
		passed: false
	},
	{
		id: 'screen-lock',
		label: 'Screen lock',
		group: 'A.9 Access Control',
		name: 'Screen lock timeout ≤ 300 seconds',
		sql: "SELECT value\n  FROM preferences\n WHERE domain = 'com.apple.screensaver'\n   AND key = 'idleTime';",
		jexl: 'results.size() > 0 && results[0].value <= 300',
		reads: 'Passes when the screen blanks after five minutes at the latest.',
		json: '[\n  { "value": "120" }\n]',
		passed: true
	},
	{
		id: 'auto-login',
		label: 'Auto-login',
		group: 'A.9 Access Control',
		name: 'Automatic login disabled',
		sql: "SELECT value\n  FROM preferences\n WHERE domain = 'com.apple.loginwindow'\n   AND key = 'autoLoginUser';",
		jexl: 'results.size() == 0',
		reads: 'Passes when no rows come back, so no auto-login user is configured. Absence is the evidence.',
		json: '[]',
		passed: true
	}
];

/** What a fresh deployment seeds on first start. */
export const catalog = [
	{
		group: 'A.9 Access Control',
		items: [
			'Screen lock timeout ≤ 300 seconds',
			'Screen lock requires a password',
			'Automatic login disabled',
			'Guest account disabled'
		]
	},
	{
		group: 'A.10 Cryptography',
		items: ['FileVault enabled']
	},
	{
		group: 'A.12 Operations Security',
		items: [
			'Firewall enabled',
			'Firewall logging enabled',
			'Automatic updates enabled',
			'Critical security updates installed automatically',
			'macOS updates installed automatically',
			'Gatekeeper enabled',
			'System Integrity Protection enabled',
			'Time Machine backup destination configured'
		]
	},
	{
		group: 'A.13 Communications Security',
		items: [
			'Remote login (SSH) disabled',
			'Screen sharing disabled',
			'File sharing disabled',
			'Internet sharing disabled',
			'Firewall stealth mode enabled'
		]
	},
	{
		group: 'A.12.6.2 Software installation',
		items: ['No blacklisted applications installed'],
		note: 'Its SQL is generated from your blocked-app rules at request time.'
	}
];
