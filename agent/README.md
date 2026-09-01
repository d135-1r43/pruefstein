# pruefstein-agent

The local compliance agent. It logs in once as the employee, downloads the
compliance checks from a Prüfstein server, runs each of them through
[osquery](https://osquery.io/), and pushes a report back.

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
| `run` | Runs every compliance check and pushes a report |
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

Needs **`osqueryi` on the `PATH`** (`brew install --cask osquery` on macOS).
Each query gets 10 seconds before it is abandoned.

`run` takes no arguments — it reads the stored server and refreshes the access
token on its own. If the refresh token is gone or rejected it falls back to an
interactive device login, which is worth knowing before putting `run` in cron
or launchd: an unattended run can end up waiting for a browser confirmation
that nobody gives.

`QUARKUS_REST_CLIENT_PRUEFSTEIN_API_URL` overrides the stored server for a
single run, which is what CI uses.

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
