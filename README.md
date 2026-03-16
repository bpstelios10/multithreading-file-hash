# multithreading-file-hash
java multithreading exercise:
* one endpoint accepts a text and returns a map of the sentence index and its hash
* uses `ExecutorService` with 2 threads for parallel computation (not configurable yet) - `newFixedThreadPool(2)`
* uses `ThreadLocal` so that each thread keeps its own `MessageDigest`. one instance would result into wrong functionality.
  as many instances as sentences would consume resources, cause MessageDigest is a heavy-to-build object.

---

### 🧵 Core Threading & Task Execution

* **ExecutorService** → thread pools, task submission, lifecycle
* **Callable vs Runnable** → returning results + checked exceptions
* **Future** → async result handling (blocking `get()`)

---

### ⚡ Modern Asynchronous Programming

* **CompletableFuture**

  * async task composition (`thenApply`, `thenCombine`, etc.)
  * non-blocking pipelines
  * `join()` vs `get()`
* **Async vs Parallel thinking** → chaining vs splitting work

---

### 🔀 Parallelism Models

* **Parallel Streams**

  * implicit ForkJoinPool usage
  * data parallelism with minimal code
* **Manual parallel decomposition**

  * splitting text → processing chunks independently

---

### 🧠 Fork/Join Framework

* **ForkJoinPool**
* **RecursiveTask** → divide & conquer with return values
* **RecursiveAction** → side-effect-based parallelism
* Work-stealing concept (implicitly used)

---

### 🧮 Concurrent Data Structures & Counters

* **ConcurrentHashMap** → thread-safe aggregation
* **LongAdder** → high-performance contention-friendly counters

---

### 🧱 Thread Confinement & Optimization

* **ThreadLocal**

  * avoiding shared mutable state
  * per-thread reuse of expensive objects (`MessageDigest`)

---

### 🔄 Map-Reduce Pattern

* Splitting work → mapping → reducing results
* Implemented via:

  * `CompletableFuture`
  * `parallelStream`
  * Fork/Join

---

### ⚖️ Concurrency Trade-offs Awareness (implicitly)

* Blocking vs non-blocking (`Future` vs `CompletableFuture`)
* Shared state vs immutable/isolated computation
* Performance vs readability (ForkJoin vs streams)
