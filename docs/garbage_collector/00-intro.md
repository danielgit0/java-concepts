# Java Garbage Collector (GC) Summary

## Overview

The Java Garbage Collector (GC) is an automatic memory management system within the Java Virtual Machine (JVM). Its primary responsibility is to reclaim memory occupied by objects that are no longer reachable by the application, reducing the need for manual memory management and helping prevent memory leaks and dangling pointer issues.

### Key Goals of Garbage Collection

- Automatically reclaim unused memory.
- Minimize application pause times.
- Maximize application throughput.
- Efficiently utilize available CPU and memory resources.
- Support applications with varying latency and scalability requirements.

---

## How Java Garbage Collection Works

### 1. Object Allocation

Most objects are allocated in the **Heap**, which is divided into generations:

- **Young Generation**
    - Eden Space
    - Survivor Spaces (S0 and S1)

- **Old (Tenured) Generation**
    - Stores long-lived objects.

- **Metaspace**
    - Stores class metadata (replaced PermGen since Java 8).

### 2. Reachability Analysis

Java determines whether an object is still in use by tracing references from **GC Roots**, such as:

- Active thread stacks
- Static variables
- JNI references
- JVM internal references

Objects that cannot be reached from any GC Root are considered garbage and become eligible for collection.

### 3. Collection Process

The JVM typically performs:

#### Minor GC
- Collects the Young Generation.
- Usually fast.
- Occurs frequently.

#### Major GC / Full GC
- Collects the Old Generation.
- More expensive.
- Can introduce noticeable pauses.

### 4. Common GC Techniques

#### Mark
Identify reachable objects.

#### Sweep
Reclaim memory occupied by unreachable objects.

#### Compact
Move surviving objects together to reduce fragmentation.

#### Copying
Copy live objects from one region to another, leaving dead objects behind.

---

## Important GC Metrics

### Throughput
Percentage of total execution time spent running application code rather than GC.

Example:

- Application: 99 seconds
- GC: 1 second

Throughput = 99%

### Latency (Pause Time)

The amount of time application threads are stopped during GC.

Lower latency is critical for:

- Financial systems
- Real-time applications
- Interactive web services

### Footprint

The total memory consumed by the JVM.

---

## Java Garbage Collectors

Modern JVMs provide several garbage collectors optimized for different workloads.

---

# Comparison of Java Garbage Collectors

| Garbage Collector | Introduced | Strategy | Advantages | Disadvantages | Best Use Cases |
|------------------|------------|----------|------------|---------------|---------------|
| Serial GC | JDK 1.3 | Single-threaded Mark-Copy / Mark-Compact | Simple, low overhead, minimal memory usage | Long pause times, poor scalability | Small applications, embedded systems, single-core environments |
| Parallel GC (Throughput GC) | JDK 1.4 | Multi-threaded collection | High throughput, efficient CPU utilization | Longer pauses than low-latency collectors | Batch processing, backend jobs, compute-intensive workloads |
| CMS (Concurrent Mark Sweep) | JDK 1.4.2 | Concurrent Mark-Sweep | Reduced pause times compared to Parallel GC | Fragmentation issues, high CPU usage, deprecated and removed | Historically used for low-latency systems |
| G1 GC | JDK 7 | Region-based incremental collection | Predictable pauses, balanced throughput and latency, handles large heaps | More complex tuning, slightly lower throughput than Parallel GC | General-purpose server applications |
| Shenandoah GC | JDK 12 (production) | Concurrent compacting collector | Very low pause times independent of heap size | Additional CPU overhead, lower throughput in some workloads | Large heaps with strict latency requirements |
| ZGC | JDK 15 (production) | Concurrent region-based collector with colored pointers | Extremely low pauses (<10 ms target), scales to very large heaps | Higher memory overhead, may reduce throughput slightly | Large-scale low-latency services |
| Epsilon GC | JDK 11 | No-op collector | Minimal GC overhead, useful for testing | Does not reclaim memory; eventually causes OutOfMemoryError | Performance testing and benchmarking |
| Generational ZGC | JDK 21 | Generational version of ZGC | Lower CPU usage than classic ZGC, low pauses, improved efficiency | Newer technology, less operational experience in some organizations | Modern low-latency applications |
| Generational Shenandoah | JDK 21 | Generational Shenandoah | Better efficiency than non-generational Shenandoah | Newer implementation with less adoption | Large heap applications requiring low pauses |

---

# Detailed Comparison

| Collector | Pause Time | Throughput | Heap Size Support | CPU Usage | Memory Overhead |
|-----------|------------|------------|------------------|-----------|----------------|
| Serial GC | High | Medium | Small | Low | Low |
| Parallel GC | Medium-High | Very High | Medium-Large | Medium | Low |
| CMS | Medium | High | Large | High | Medium |
| G1 GC | Low-Medium | High | Large | Medium | Medium |
| Shenandoah | Very Low | Medium-High | Very Large | High | Medium |
| ZGC | Extremely Low | Medium-High | Extremely Large | Medium-High | Medium-High |
| Generational ZGC | Extremely Low | High | Extremely Large | Medium | Medium |
| Generational Shenandoah | Very Low | High | Very Large | Medium-High | Medium |

---

# Current Recommendations (Java 21+)

| Scenario | Recommended GC |
|-----------|---------------|
| General-purpose server application | G1 GC |
| Maximum throughput | Parallel GC |
| Large heap with low-latency requirements | Generational ZGC |
| Very large heap with strict pause-time requirements | Generational ZGC |
| Large heap with low-latency and open-source focus | Generational Shenandoah |
| Small JVM or containerized service | G1 GC |
| Benchmarking without GC interference | Epsilon GC |

---

# Common JVM Options

## G1 GC (Default in Modern JDKs)

```bash
java -XX:+UseG1GC MyApplication
```

## Parallel GC

```bash
java -XX:+UseParallelGC MyApplication
```

## Serial GC

```bash
java -XX:+UseSerialGC MyApplication
```

## ZGC

```bash
java -XX:+UseZGC MyApplication
```

## Shenandoah

```bash
java -XX:+UseShenandoahGC MyApplication
```

---

# Default Garbage Collector by Java Version

| Java Version | Default GC |
|--------------|------------|
| Java 8 | Parallel GC |
| Java 9+ | G1 GC |
| Java 17 | G1 GC |
| Java 21 | G1 GC |

---

# Collector Selection Guide

```text
Need maximum throughput?
└── Parallel GC

Need general-purpose production GC?
└── G1 GC

Need ultra-low latency (<10ms pauses)?
└── Generational ZGC

Need low latency on very large heaps?
└── Generational ZGC or Generational Shenandoah

Need smallest footprint?
└── Serial GC

Need GC-free benchmarking?
└── Epsilon GC
```

---

# Key Takeaways

1. Java GC automatically manages heap memory by reclaiming unreachable objects.
2. Modern collectors prioritize different tradeoffs between throughput, latency, CPU usage, and memory consumption.
3. G1 GC is the default and recommended starting point for most applications.
4. Parallel GC remains a strong choice for maximum throughput workloads.
5. Generational ZGC is currently the preferred collector for large-scale low-latency applications on Java 21+.
6. Shenandoah provides similar low-latency characteristics with a different implementation approach.
7. GC selection should be based on application requirements rather than benchmark results alone.

---
**Rule of Thumb**

- Start with **G1 GC**.
- Move to **Parallel GC** if throughput is the primary goal.
- Move to **Generational ZGC** if latency is the primary goal.
- Consider **Generational Shenandoah** if you need low latency and it performs better in your environment.
