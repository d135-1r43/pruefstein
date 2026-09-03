# pruefstein-agent

The local compliance agent. It logs in once as the employee, downloads the
compliance checks from a Prüfstein server, runs each of them through
[osquery](https://osquery.io/), shows what they found — and only then asks
whether to report it.

Built with Quarkus and [Picocli](https://quarkus.io/guides/picocli).

---

## Installing

From this directory:

```bash
./bin/install.sh
```

(or `./agent/bin/install.sh` from the repository root). This builds the agent
if nothing is built yet and makes it available as the
`pruefstein-agent` command. `./bin/install.sh --uninstall` removes it again.
See the [root README](../README.md#building-and-installing-the-cli) for
prerequisites and the full first-run walkthrough.

## Commands

| Command | What it does |
|---|---|
| `login` | Authenticates against the server's identity provider and caches the credentials |
| `run` | Runs every compliance check, then asks whether to report the result |
| `logout` | Deletes the cached credentials |

```bash
pruefstein-agent login --server https://pruefstein.example.com
pruefstein-agent run
pruefstein-agent logout
```

Every command accepts `--help`.

### login

`--server` (`-s`) names the Prüfstein server to report to. It is only needed
the first time: the URL is stored alongside the credentials and reused by every
later run. Naming a *different* server discards the cached token — it was
issued by the previous server's identity provider and means nothing to the new
one.

Login uses the OAuth **device code** flow: the agent prints a URL and a code,
you confirm in the browser, and the token comes back to the agent. The agent
itself carries no identity-provider configuration — it asks the server
(`GET /internal/agent-config`) which issuer and client id to use, so the same
binary works against Keycloak and Microsoft Entra ID without a rebuild.

Everything ends up in `~/.config/pruefstein/credentials.json`: server URL,
issuer, access token and refresh token.

### run

A run is two separate things: checking the machine, and reporting on it. The
checks run first and print their verdicts, and nothing has reached the server
at that point — the only thing `run` needed it for was the list of checks. Then
it asks:

```
  [PASS] FileVault enabled
  [FAIL] Firewall enabled
────────────────────────────────────────────
Done: 3/4 checks passed
Report this run? [y/N]
```

Answer no and nothing is filed: turn the firewall on, run again, and report the
run you would rather stand behind. Answer yes and the report exists — there is
no unsending it, which is why the question is asked at the one moment when the
verdicts are known and the server still knows nothing.

Anything other than `y` or `yes` is a no, a bare Enter included.

**Unattended runs need `--yes`.** With stdin closed — cron, launchd, CI — the
prompt reaches EOF, and an EOF is not consent: nothing is reported and `run`
exits non-zero, so a schedule that is quietly reporting nothing looks broken
rather than healthy.

```bash
pruefstein-agent run --yes    # or -y
```

Needs **`osqueryi` on the `PATH`**. When it is missing, `run` asks before
doing anything else:

```
You need osqueryi to continue, install it? [y/n]
```

A `y` runs `brew install --cask osquery` in the foreground — Homebrew installs
a signed pkg, so it will ask for a sudo password. Anything else, a bare Enter
included, aborts with exit code 1 and installs nothing. An unattended `run`
never reaches the prompt: with stdin closed it aborts on the spot rather than
waiting for an answer, so cron gets a non-zero exit instead of a report in
which every check errored.

Each query gets 10 seconds before it is abandoned.

Checks run **concurrently**. Each one is its own short-lived `osqueryi`
process and nearly all of the cost is process startup — 12 invocations
measured 3.2 s one after another against 0.31 s at once — so a run finishes in
about the time its slowest check takes. Concurrent `osqueryi` instances do not
contend for anything: unlike `osqueryd` it keeps its database in memory.

```bash
PRUEFSTEIN_AGENT_CHECK_PARALLELISM=4 pruefstein-agent run   # gentler on the machine
```

`pruefstein.agent.check-parallelism` (default 100) is a cap, not a target — a
run never starts more processes than it has checks. Results keep the order the
server sent them in; only the `[PASS]`/`[FAIL]` lines arrive as each check
finishes.

Apart from `--yes`, `run` takes no arguments — it reads the stored server and
refreshes the access token on its own. If the refresh token is gone or rejected
it falls back to an interactive device login, which is worth knowing before
putting `run` in cron or launchd: an unattended run can end up waiting for a
browser confirmation that nobody gives.

The token is only needed at two points — fetching the checks and filing the
report — and each is retried on its own if the server rejects the credentials.
A token that expires while someone thinks about the question therefore costs
them the wait for a refresh, not the run.

`QUARKUS_REST_CLIENT_PRUEFSTEIN_API_URL` overrides the stored server for a
single run, which is what CI uses.

---

## Output

The commands print their own progress and nothing else — no timestamps, no
logger names, no framework startup lines. Someone running a compliance check
does not need to know which profile is active or which Quarkus features are
installed, so the root logger sits at `WARN` and only `com.pruefstein` logs at
`INFO`.

Verdicts are coloured: `[PASS]` green, `[FAIL]` and `[ERROR]` red. The closing
summary is bold, and green when the whole run passed; a run with failures is
left in the terminal's normal text colour, since the red already sits on the
`[FAIL]` lines that name the checks. A faint rule separates it from the
per-check lines:

```
  [PASS] FileVault enabled
  [FAIL] Firewall enabled
────────────────────────────────────────────
Done: 2/4 checks passed
Report this run? [y/N]
```

Colour is decided by picocli's `Ansi.AUTO`, so it turns itself off when there
is no terminal — piped into a file, mailed by cron, or with `NO_COLOR=1` set —
rather than writing escape codes into a log.

Quarkus's own log colourization is off (`quarkus.console.color=false`). It
paints messages in 256-colour greys and near-whites chosen for a dark
terminal — `38;5;231`, `38;5;251`, `38;5;253` — which read as washed-out grey
on a light background and sit on top of the colours above. With it off the
message text reaches the terminal exactly as written.

When something needs diagnosing, turn it back up for one run:

```bash
QUARKUS_LOG_LEVEL=INFO pruefstein-agent run                        # framework startup lines back
QUARKUS_LOG_CATEGORY__COM_PRUEFSTEIN__LEVEL=DEBUG pruefstein-agent login  # why a refresh failed
QUARKUS_BANNER_ENABLED=true pruefstein-agent                       # the banner, if you miss it
```

The agent's own `DEBUG` needs that second form, not `QUARKUS_LOG_LEVEL=DEBUG`.
An explicit category level pins its subtree, and `com.pruefstein` is pinned to
`INFO` here to keep the run output visible while the root logger stays at
`WARN` — so raising the root raises everything except the agent. Either way no
rebuild is needed, because `quarkus.log.min-level` stays at `DEBUG`; the reason
a refresh was rejected is only ever logged at that level.

---

## Running without installing

The launcher is a convenience, not a requirement. After a build you can always
invoke the artifact directly:

```bash
java -jar target/quarkus-app/quarkus-run.jar login --server http://localhost:8080
./target/pruefstein-agent-1.0.0-SNAPSHOT-runner run     # after a native build
```

In dev mode, arguments are passed through `quarkus.args`:

```bash
./mvnw quarkus:dev -Dquarkus.args='run'
```

Dev mode runs the application and restarts it on Enter.

## Building

`bin/install.sh` builds for you; these are for when you want a specific
packaging:

```bash
./mvnw package            # fast-jar in target/quarkus-app/ — what the installer builds
./mvnw package -Dnative   # native binary; the launcher prefers it, nothing to reinstall
./mvnw test
```

## Related guides

- [Picocli](https://quarkus.io/guides/picocli) — the CLI framework
- [OpenID Connect Client](https://quarkus.io/guides/security-openid-connect-client) — token handling
- [Maven tooling](https://quarkus.io/guides/maven-tooling) — native builds
