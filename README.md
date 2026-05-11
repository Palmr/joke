# Joke

Java Only KDB+ client library

Originally https://github.com/KxSystems/javakdb but with more Java standards, fewer features, and (maybe one day) much less pressure on the GC.

## Main differences from official client

 - Joke only connects via TCP, no TLS/UDS
 - Joke is made for single-threaded use only
 - Joke only supports sync messages right now due to single threaded nature
 - Joke doesn't support compression yet
 - Joke has a fixed message buffer size for query and response
 - Joke has somewhat readable code
 - Joke is not ready for production use, it's just a more Java-esque starting point for some ideas I had

## Local Development Setup

### Installing kdb+

Download the free personal edition of kdb+ from [kx.com](https://kx.com/kdb-personal-edition-download/). A licence is required and is tied to your machine — it expires annually so you'll need to re-download it each year.

Follow the install instructions, unpacking the `l64.zip` and put the `kc.lic` in that folder.

### Starting kdb+

The free licence refuses to start when it detects too many CPU cores, so use `taskset` to limit the visible core count:

```bash
export QHOME=/path/to/unpacked/l64
taskset -c 0-8 ${QHOME}/l64/q
```

### Opening a port for the Java client

Once the q REPL is open, run:

```
\p 5010
```

This tells kdb+ to listen on TCP port 5010, which is the default port the example client connects to.
