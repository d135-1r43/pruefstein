#!/usr/bin/env bash
#
# Install the Prüfstein agent as the `pruefstein-agent` command.
#
#   ./agent/bin/install.sh              build if needed, then install
#   ./agent/bin/install.sh --uninstall  remove the installed command
#
# Installs a symlink into whichever directory is already on your PATH, so
# there is normally nothing to add to your shell profile. The link points
# back into this working tree: rebuild and the new version is picked up
# with nothing to reinstall.
#
set -euo pipefail

command_name="pruefstein-agent"

# Resolve through any symlink to this script's real home.
src="${BASH_SOURCE[0]}"
while [[ -L "$src" ]]; do
	dir="$(cd -P "$(dirname "$src")" && pwd)"
	src="$(readlink "$src")"
	[[ "$src" != /* ]] && src="$dir/$src"
done
module="$(cd -P "$(dirname "$src")/.." && pwd)"
launcher="$module/bin/$command_name"

on_path()
{
	case ":$PATH:" in
		*":$1:"*) return 0 ;;
		*) return 1 ;;
	esac
}

# Prefer a directory that is already on PATH and writable without sudo.
pick_target_dir()
{
	local candidate
	for candidate in "$HOME/.local/bin" "$HOME/bin" /usr/local/bin; do
		if on_path "$candidate" && [[ -d "$candidate" && -w "$candidate" ]]; then
			echo "$candidate"
			return
		fi
	done
	# Nothing ready: fall back to ~/.local/bin and create it. The PATH hint
	# below tells the user what to add if it is not picked up.
	echo "$HOME/.local/bin"
}

target_dir="$(pick_target_dir)"
link="$target_dir/$command_name"

if [[ "${1:-}" == "--uninstall" ]]; then
	for candidate in "$HOME/.local/bin" "$HOME/bin" /usr/local/bin; do
		if [[ -L "$candidate/$command_name" ]]; then
			rm -f "$candidate/$command_name"
			echo "Removed $candidate/$command_name"
		fi
	done
	echo "Done. The build in agent/target was left alone."
	exit 0
fi

# ── 1. make sure something is built ────────────────────────────────────
target="$module/target"
built="$(find "$target" -maxdepth 1 -type f -name 'pruefstein-agent-*-runner' 2>/dev/null | head -n1 || true)"
if [[ -z "$built" ]]; then
	built="$(find "$target" -maxdepth 1 -type f -name 'pruefstein-agent-*-runner.jar' 2>/dev/null | head -n1 || true)"
fi
if [[ -z "$built" && -f "$target/quarkus-app/quarkus-run.jar" ]]; then
	built="$target/quarkus-app/quarkus-run.jar"
fi

if [[ -z "$built" ]]; then
	echo "No build found — running ./mvnw package (this takes a minute)…"
	(cd "$module" && ./mvnw package -q -DskipTests)
fi

chmod +x "$launcher"

# ── 2. link it onto the PATH ───────────────────────────────────────────
mkdir -p "$target_dir"
ln -sfn "$launcher" "$link"
echo "Linked $link -> $launcher"

# ── 3. verify, and say exactly what is missing if it did not take ──────
if ! on_path "$target_dir"; then
	cat <<MSG

$target_dir is not on your PATH yet. Add this line to your shell profile
(~/.zshrc on macOS), then open a new terminal:

    export PATH="$target_dir:\$PATH"
MSG
	exit 0
fi

hash -r 2>/dev/null || true
if resolved="$(command -v "$command_name" 2>/dev/null)"; then
	echo "Installed: $command_name -> $resolved"
	echo
	"$command_name" --help || true
else
	echo "Linked, but the shell does not see it yet — open a new terminal." >&2
fi
