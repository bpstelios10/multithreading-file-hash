# multithreading-file-hash

java multithreading exercise:

* one endpoint accepts a text and returns a map of the sentence index and its hash
* uses `ExecutorService` with 2 threads for parallel computation (not configurable yet) - `newFixedThreadPool(2)`
* uses `ThreadLocal` so that each thread keeps its own `MessageDigest`. one instance would result into wrong
  functionality.
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

---

### Locking

Locks (specifically from the java.util.concurrent.locks package) are considered the more modern and flexible alternative
to synchronized blocks. They offer

1) fairness: can configure to first-comes first-served
2) tryLock(): try to acquire lock and if it is not available then the thread can do something else or wait for X time
3) memory visibility: before lock is released, the changes are flushed to main memory & are available for other threads
4) optimistic locking: generally locks & synchronized blocks lock first, but there are some locks that don't, so they
   improve performance

Most common locks:

* ReentrantLock: A flexible, exclusive lock that allows a single thread to hold a resource, supporting features like
  fairness and timeouts. (modern way for Synchronized blocks)
  Use case: Protecting a shared resource where you need to prevent "deadlocks" or handle high-contention logic.

* ReentrantReadWriteLock: A dual-lock system that allows multiple readers simultaneously but only one writer at a time.
  The readers are still using a lock, but they all share the 1. There is another one lock for writing.
  Use case: A shared Cache or Configuration object where reads happen much more often than updates.

* StampedLock: An advanced lock that supports Optimistic Reading, allowing readers to grab data without blocking writers
  at all. Here, there are NO LOCKS used! So much better performance than ReentrantReadWriteLock.
  Use case: High-performance systems (like Exchange Engines) where you cannot afford to have readers slow down writers.

* Semaphore: A "counter-based" lock that limits access to a fixed number of N threads rather than just one.
  Use case: Throttling access to a limited pool of resources, such as a Database Connection Pool or an external API.

---

### ⚡ Atomic Variables

* Atomics are used for Compare-And-Swap (CAS) operations, which ensures the entire sequence happens as a single,
  uninterruptible step. They are thread-safe, without using any Locks.

---

### 🧠 BlockingQueue

A `Queue` that additionally supports operations that wait for the queue to become non-empty when retrieving an element.
`put()` method blocks the current thread indefinitely until the operation can succeed.
`BlockingQueue` implementations are thread-safe. All queuing methods achieve their effects atomically using internal
locks or other forms of concurrency control.

* **Backpressure** new LinkedBlockingQueue<>(100);

  If workers are slow → producer blocks

  If aggregator is slow → workers block

  Natural flow control
* **Pipeline Parallelism**

  Reader, workers, aggregator all run concurrently

  Different stages can scale independently
* Coordination (Important!)

  Poison pill pattern

  Workers notify aggregator when done

---

### Controller returning CompletableFuture

In Spring MVC:
Request comes in → handled by a servlet thread
If you return CompletableFuture (called Servlet async processing):
Spring detaches the request → releases the thread → waits for completion in the background → resumes when done

✅ Big win when:

* your service does IO (DB, API calls, files)
* you have many concurrent requests
* tasks take noticeable time

But with an important nuance:

Tomcat itself is not “async” in the reactive sense. It uses the Servlet async API (Servlet 3.0+). So Tomcat is doing
thread handoff, not non-blocking IO magic.

---

### TTL cache

A concurrency-aware async cache that stores `key -> CompletableFuture<value>`. This way multiple concurrent callers
should not recompute the same thing 3 times. Instead, the first thread computes the value and the rest reuse it, which
is called **request coalescing** (used in Guava Cache, Caffeine, HTTP request deduplication layers, gateways, and more).

A TTL cache with ConcurrentHashMap + CompletableFuture + computeIfAbsent is a perfect concurrency exercise. It combines:

* lock-free concurrency
* memoization
* async coordination
* duplicate-work suppression
* expiration logic
* safe publication

---