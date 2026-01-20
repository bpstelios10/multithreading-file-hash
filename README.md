# multithreading-file-hash
java multithreading exercise:
* one endpoint accepts a text and returns a map of the sentence index and its hash
* uses `ExecutorService` with 2 threads for parallel computation (not configurable yet) - `newFixedThreadPool(2)`
* uses `ThreadLocal` so that each thread keeps its own `MessageDigest`. one instance would result into wrong functionality.
  as many instances as sentences would consume resources, cause MessageDigest is a heavy-to-build object.
