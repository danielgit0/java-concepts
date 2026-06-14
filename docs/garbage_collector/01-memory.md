# Java JVM Memory Settings Summary

## Overview

The Java Virtual Machine (JVM) uses several memory areas to manage application execution. Proper memory configuration is critical for:

- Application performance
- Startup time
- Throughput
- Garbage Collection behavior
- Container and cloud deployments
- Preventing OutOfMemoryError (OOM)

The most important JVM memory settings control the **Heap**, **Metaspace**, and **Thread Stack** sizes.

---

# JVM Memory Areas

```text
+------------------------------------------------+
|                    JVM                         |
+------------------------------------------------+
|                                                |
|  Heap                                          |
|   +----------------------+                     |
|   | Young Generation     |                     |
|   | Eden + Survivors     |                     |
|   +----------------------+                     |
|   | Old Generation       |                     |
|   +----------------------+                     |
|                                                |
|  Metaspace                                    |
|                                                |
|  Thread Stacks                                |
|                                                |
|  Code Cache                                   |
|                                                |
+------------------------------------------------+
```

---

# Important Memory Parameters

## Heap Size

The heap stores Java objects.

### Initial Heap Size

```bash
-Xms<size>
```

Example:

```bash
-Xms2g
```

Starts JVM with a 2 GB heap.

---

### Maximum Heap Size

```bash
-Xmx<size>
```

Example:

```bash
-Xmx8g
```

Maximum heap can grow to 8 GB.

---

### Best Practice

For production systems:

```bash
-Xms8g -Xmx8g
```

Keeping both values equal:

- Eliminates heap resizing
- Improves predictability
- Reduces GC overhead

---

## Metaspace

Stores:

- Class metadata
- Reflection information
- Runtime-generated classes

### Limit Metaspace

```bash
-XX:MaxMetaspaceSize=512m
```

Example:

```bash
-XX:MaxMetaspaceSize=1g
```

---

## Thread Stack Size

Each Java thread receives its own stack.

```bash
-Xss<size>
```

Example:

```bash
-Xss1m
```

Common values:

| Value | Usage |
|---------|---------|
| 256k | Very high thread count |
| 512k | Moderate thread count |
| 1m | Default recommendation |
| 2m+ | Deep recursion |

---

## Direct Memory

Used by:

- NIO
- Netty
- Kafka
- High-performance networking

```bash
-XX:MaxDirectMemorySize=2g
```

Example:

```bash
-XX:MaxDirectMemorySize=4g
```

---

## Code Cache

Stores JIT-compiled machine code.

```bash
-XX:ReservedCodeCacheSize=512m
```

Usually only tuned for:

- Large applications
- Trading systems
- JVM-heavy workloads

---

# Memory Sizing Strategy

## Rule #1

Never allocate all physical memory to the heap.

Operating system and native memory need space too.

Bad:

```text
Server RAM: 32 GB
Heap: 32 GB
```

Good:

```text
Server RAM: 32 GB
Heap: 24 GB
```

---

## Rule #2

Account for Non-Heap Memory

Total JVM memory is approximately:

```text
Heap
+ Metaspace
+ Thread Stacks
+ Direct Memory
+ Code Cache
+ Native Libraries
--------------------------------
Total Process Memory
```

Example:

```text
Heap               8 GB
Metaspace          512 MB
Direct Memory      2 GB
Threads            500 MB
Code Cache         256 MB
--------------------------------
Total             ~11.3 GB
```

---

# Hardware-Based Examples

---

# Example 1: Small Development Machine

### Hardware

```text
RAM: 8 GB
CPU: 4 cores
```

### Workload

- Spring Boot application
- Local development
- Docker Desktop running

### Recommended Settings

```bash
-Xms512m
-Xmx2g
-XX:MaxMetaspaceSize=512m
```

### Why?

Leave memory available for:

- IDE
- Browser
- Docker
- Database

Memory usage:

```text
Total RAM        8 GB
JVM Heap         2 GB
Other Processes  6 GB
```

---

# Example 2: Medium Application Server

### Hardware

```text
RAM: 16 GB
CPU: 8 cores
```

### Workload

- REST APIs
- Moderate traffic
- Spring Boot

### Recommended Settings

```bash
-Xms4g
-Xmx4g
-XX:MaxMetaspaceSize=512m
```

### Expected Usage

```text
Heap             4 GB
Metaspace        512 MB
Direct Memory    512 MB
OS/Cache         Remaining
```

---

# Example 3: Enterprise Application Server

### Hardware

```text
RAM: 32 GB
CPU: 16 cores
```

### Workload

- Multiple APIs
- High throughput
- G1 GC

### Recommended Settings

```bash
-Xms16g
-Xmx16g
-XX:MaxMetaspaceSize=1g
```

### Why Not 32 GB Heap?

Need memory for:

- OS
- Filesystem cache
- Native libraries
- Monitoring agents

Recommended allocation:

```text
Heap             16 GB
Other JVM         2 GB
OS + Cache       14 GB
```

---

# Example 4: Large Service Using G1 GC

### Hardware

```text
RAM: 64 GB
CPU: 32 cores
```

### Workload

- Large cache
- Microservices
- High request volume

### Recommended Settings

```bash
-Xms32g
-Xmx32g
-XX:+UseG1GC
```

### Notes

Above 32 GB heap:

- Object references become larger unless compressed oops can still be used.
- More memory is not always faster.

Many teams prefer:

```text
Heap = 24–32 GB
```

even on larger machines.

---

# Example 5: Low-Latency Service with ZGC

### Hardware

```text
RAM: 128 GB
CPU: 64 cores
```

### Workload

- Trading platform
- Real-time analytics
- Latency-sensitive systems

### Recommended Settings

```bash
-Xms64g
-Xmx64g
-XX:+UseZGC
```

### Benefits

- Sub-millisecond to low-millisecond pauses
- Huge heap support
- Predictable latency

---

# Example 6: Kafka Consumer / Producer

### Hardware

```text
RAM: 16 GB
```

### Workload

- High throughput messaging

### Recommended Settings

```bash
-Xms4g
-Xmx4g
-XX:MaxDirectMemorySize=4g
```

### Why?

Kafka uses:

- Direct ByteBuffers
- NIO networking

Direct memory becomes important.

---

# Example 7: Application with Many Threads

### Hardware

```text
RAM: 32 GB
```

### Workload

```text
3000 Threads
```

### Configuration

```bash
-Xss256k
```

Memory consumed by stacks:

```text
3000 × 256 KB
≈ 750 MB
```

Using:

```bash
-Xss1m
```

would require:

```text
3000 × 1 MB
≈ 3 GB
```

---

# Containerized Applications (Docker/Kubernetes)

## Common Mistake

Container:

```text
Memory Limit: 2 GB
```

JVM:

```bash
-Xmx2g
```

Result:

```text
Container OOM Kill
```

because JVM also needs:

- Metaspace
- Direct Memory
- Thread Stacks

---

## Recommended

Container:

```text
Memory Limit: 2 GB
```

JVM:

```bash
-Xms1g
-Xmx1g
```

or

```bash
-XX:MaxRAMPercentage=50
```

---

# Modern Container-Friendly Configuration

Java 17+:

```bash
-XX:InitialRAMPercentage=50
-XX:MaxRAMPercentage=70
```

Example:

```text
Container Limit = 8 GB
```

Heap:

```text
Max Heap ≈ 5.6 GB
```

Automatically calculated.

---

# OutOfMemoryError Types

| Error | Cause |
|---------|---------|
| Java heap space | Heap too small |
| GC overhead limit exceeded | JVM spends most time collecting |
| Metaspace | Class metadata exhausted |
| Direct buffer memory | Direct memory exhausted |
| Unable to create new native thread | OS thread limit or memory exhaustion |
| StackOverflowError | Deep recursion or stack too small |

---

# Quick Sizing Cheat Sheet

| Server RAM | Recommended Heap | Typical Use Case |
|------------|-----------------|------------------|
| 4 GB | 1–2 GB | Development |
| 8 GB | 2–4 GB | Small services |
| 16 GB | 4–8 GB | APIs and microservices |
| 32 GB | 8–16 GB | Enterprise applications |
| 64 GB | 16–32 GB | Large services |
| 128 GB+ | 32–64 GB | Low-latency systems, large caches |

---

# Recommended Defaults (Java 21+)

## Small Service

```bash
-Xms1g
-Xmx1g
-XX:+UseG1GC
```

## Standard Production API

```bash
-Xms4g
-Xmx4g
-XX:+UseG1GC
```

## Large Enterprise Service

```bash
-Xms16g
-Xmx16g
-XX:+UseG1GC
```

## Low-Latency Service

```bash
-Xms32g
-Xmx32g
-XX:+UseZGC
```

## Kubernetes Deployment

```bash
-XX:InitialRAMPercentage=50
-XX:MaxRAMPercentage=70
```

---

# Key Takeaways

1. The heap is only part of JVM memory usage.
2. Always leave memory for the OS and native allocations.
3. Production systems typically use `-Xms = -Xmx`.
4. G1 GC is the best default choice for most workloads.
5. ZGC is preferred when latency is more important than maximum throughput.
6. Container memory limits must account for heap and non-heap memory.
7. Monitor actual memory usage before increasing heap size; many performance issues are caused by object allocation patterns rather than insufficient heap.
