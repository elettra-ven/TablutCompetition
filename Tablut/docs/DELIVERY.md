# AliEleGio player delivery

## Install in the tournament VM

Build from the project directory (`Tablut/`) with the VM offline:

```sh
ant clean compile
chmod +x runmyplayer.sh
```

Copy the runtime files to the organizer's player directory. Replace `SOURCE_DIR` with the directory containing this repository; `scp` is shown as an example when the VM is reachable over SSH:

```sh
ssh tablut@VM_HOST 'mkdir -p /home/tablut/tablut && rm -f /home/tablut/tablut/RandomPlayer.jar'
scp -r build lib runmyplayer.sh tablut@VM_HOST:/home/tablut/tablut/
ssh tablut@VM_HOST 'chmod +x /home/tablut/tablut/runmyplayer.sh'
```

If copying from inside the VM, copy the same three items into `/home/tablut/tablut/` and remove the professors' `RandomPlayer.jar` there. The script changes to its own directory, so the launch working directory does not matter. Keep the provided `lib/` folder beside `build/`; no downloads are needed.

## Run

The organizer command is:

```sh
/home/tablut/tablut/runmyplayer.sh WHITE 60 SERVER_IP
```

`WHITE` may be `BLACK` and the role is case-insensitive (for example, `wHitE`). The timeout is in seconds and the third argument is the server IP. The client reserves time from the supplied timeout for JVM and protocol overhead. To run directly with Ant during local server testing:

```sh
cd /home/tablut/tablut
ant team -Drole=WHITE -Dtimeout=60 -Dip=SERVER_IP
```

The optional diagnostic line per move is enabled with `-Dalielegio.verbose=true` when launching Java directly, or by passing `true` as an optional fourth argument to `OurClient`. Logging is quiet by default.

## Offline and startup checklist

- VM has a 64-bit Java 8 runtime (`java -version` should report 1.8); no internet access or runtime download is used.
- `/home/tablut/tablut/build/`, `/home/tablut/tablut/lib/`, and executable `/home/tablut/tablut/runmyplayer.sh` are present.
- `lib/gson-2.2.2.jar` and `lib/commons-cli-1.4.jar` are shipped with the project; the launch script uses `build:lib/*`.
- The script sets `-Xmx256m`. Search's transposition table is about 22 MiB; 256 MiB leaves room for the client, search buffers, and JVM overhead.
- Launch accepts full `WHITE` or `BLACK` words in any case, uses the supplied timeout and server IP, and identifies as `AliEleGio`.
- Standard output is quiet during normal play; verbose diagnostics are opt-in. The protocol loop blocks in `read()` while the opponent is thinking.
- The GA champion weights are embedded in `Weights.java`; the player performs no weight-file reads at runtime.

## Virtuale submission text template

Save a plain text file and replace the placeholders:

```text
Team: AliEleGio
VM link: <paste the shared VM link here>
Run command: /home/tablut/tablut/runmyplayer.sh <WHITE|BLACK> <TIMEOUT_SECONDS> <SERVER_IP>
Optional parameters: none
```
