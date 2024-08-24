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
