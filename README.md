# Joke

Java Only KDB+ client library

Originally https://github.com/KxSystems/javakdb but with more Java standards, fewer features, and (maybe one day) much less pressure on the GC.

It might sound like a joke, but it's seriously better.

## Benchmarks

Measured on 10,000-element arrays using JMH (5 iterations, `AverageTime` mode) with the GC profiler.
Compared against the [official KxSystems Java driver](https://github.com/KxSystems/javakdb).

### Serialize

| Operation  | Joke (µs/op) | Official (µs/op) | Speedup   | Joke alloc (B/op) | Official alloc (B/op) |
|------------|-------------:|-----------------:|----------:|------------------:|----------------------:|
| `long[]`   | 1.9          | 15.9             | **8.5x**  | ~0                | 80,032                |
| `double[]` | 1.9          | 23.3             | **12.1x** | ~0                | 80,032                |
| `String[]` | 90.8         | 156.8            | **1.7x**  | 240,000           | 558,920               |

Joke serializes into a reused `ByteBuffer`, so primitive array serialization allocates nothing.
The official driver allocates a fresh `byte[]` on every call.

### Deserialize

| Operation  | Joke (µs/op) | Official (µs/op) | Speedup  | Joke alloc (B/op) | Official alloc (B/op) |
|------------|-------------:|-----------------:|---------:|------------------:|----------------------:|
| `long[]`   | 5.3          | 25.2             | **4.8x** | 80,016            | 80,016                |
| `double[]` | 5.3          | 25.1             | **4.7x** | 80,072            | 80,016                |
| `String[]` | 97.0         | 92.3             | **1.0x** | 520,016           | 520,016               |

Primitive array deserialization uses bulk `ByteBuffer` reads, giving a ~5x speedup with identical
allocation (the output array itself is unavoidable). Symbol (`String[]`) deserialization is at parity
with the official driver on both time and allocation.

## Current differences from official client

- Joke only connects via TCP, no TLS/UDS
- Joke is made for single-threaded use only
- Joke only supports sync messages right now due to single threaded nature
- Joke doesn't support compression yet
- Joke has a fixed message buffer size for query and response
- Joke has somewhat readable code
- Joke is not ready for production use, it's just a more Java-esque starting point for some ideas I had

## Roadmap

### Compression
IPC compression is stubbed out and will throw if triggered. Implementing it would remove the
message-size restriction and make large payloads practical. The wire format is documented in the
[KX IPC spec](https://code.kx.com/q/basics/ipc/#compression).

### Async messaging
The current design is intentionally single-threaded: one buffer, one thread, synchronous
request/response only. Supporting async (fire-and-forget sends and unsolicited inbound messages)
requires rethinking the threading model — likely a dedicated reader thread and a callback or
`CompletableFuture`-based API — without compromising the low-allocation path for the common
synchronous case.

### Publish to a KDB+ consumer
Sending async (one-way) messages — e.g. writing to a tickerplant — only requires sending with
message type `async` and not waiting for a response. This is a small addition on top of the current
sync path but needs the API surface to expose it cleanly.

### Subscribe to a KDB+ publisher
Receiving a stream of unsolicited async messages from a KDB+ publisher (e.g. via `.u.sub`) requires
the dedicated reader thread from the async item above, plus a dispatch mechanism to route inbound
messages to application callbacks. This and async messaging are the two items most likely to be
designed together.

### TLS / Unix Domain Sockets
The official driver supports both. The goal is to bring them back cleanly using modern Java APIs —
TLS via `SSLEngine` with `SocketChannel` wrapping, and UDS via `UnixDomainSocketAddress`
(available since Java 16) rather than the reflection-based workarounds in the original.

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
