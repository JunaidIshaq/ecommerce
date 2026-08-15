# Fixing a Broken Multi-Module Spring Boot Test Suite: A Java 17 Toolchain Story

**#Java #Spring Boot #Maven #Testing #Backend #Microservices**

Ever run `mvn test` on a microservices monorepo and watch it fail before a single meaningful test even executes? Here's how I diagnosed and fixed two layered problems in a 17-module e-commerce platform — without changing a single line of business logic.

---

## The Symptom

A clean `git status`, then a simple `mvn test`... and an immediate wall of red:

```
com/shopfast/common/utils/PasswordEncryptionUtilTest has been compiled by a
more recent version of the Java Runtime (class file version 61.0), this version
of the Java Runtime only recognizes class file versions up to 60.0
```

Translation: someone compiled with **Java 17**, but the test JVM was **Java 16**.

## Root Cause #1 — The JDK Mismatch

The project requires Java 17 (`java.version=17` in the parent POM), but the machine's `JAVA_HOME` pointed at a custom JDK 16 install. Maven ran fine, but when **Surefire forked its test JVM**, it inherited that wrong JDK — so freshly compiled class files (v61) were unreadable by the v60 runtime.

### The Fix: Maven Toolchains

Instead of mutating the global environment (which breaks other projects), I pinned the build itself to JDK 17:

**`backend/pom.xml`** — added the `maven-toolchains-plugin` to the build:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-toolchains-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals><goal>toolchain</goal></goals>
        </execution>
    </executions>
    <configuration>
        <toolchains>
            <jdk>
                <version>17</version>
                <vendor>openjdk</vendor>
            </jdk>
        </toolchains>
    </configuration>
</plugin>
```

**`~/.m2/toolchains.xml`** — declared the actual JDK 17 location:

```xml
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>17</version>
            <vendor>openjdk</vendor>
        </provides>
        <configuration>
            <jdkHome>/usr/lib/jvm/java-17-openjdk-amd64</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

Now the compiler **and** the forked test JVM both consistently run on Java 17 — no matter what `JAVA_HOME` says. 🔧

## Root Cause #2 — A Deserialization Bug in the Cart Service

With JDK 17 in place, the build actually progressed — and surfaced a *real* code defect in `cart-service`:

```
InvalidDefinitionException: Cannot construct instance of
`com.shopfast.cartservice.dto.CartItemDto`
(no Creators, like default constructor, exist)
```

`CartService.parse()` serializes cart items to JSON in Redis and deserializes them back. The DTO only had Lombok's `@Builder`, which generates **no default constructor** — so Jackson had nothing to instantiate it with. Four integration-style tests were failing.

### The Fix: Jackson-friendly Lombok

```java
@Data
@Builder
@NoArgsConstructor   // Jackson needs this to instantiate
@AllArgsConstructor  // @Builder needs this for its constructor
public class CartItemDto implements Serializable { ... }
```

Two annotations, and the whole cart flow (add, merge, get, cap-at-999) works again.

## Root Cause #3 — A Test Asserting the Wrong Mock

One failure remained:

```
Wanted but not invoked: hashOperations.delete("cart:guest:...")
```

The production code deletes the guest cart after a merge via `redisTemplate.delete(gKey)` (CartService.java:290) — consistent with the rest of the suite, which verifies whole-key deletes through `redisTemplate`. The test, however, asserted `hashOperations.delete(...)`. The *behavior* was correct; the *assertion* was wrong.

### The Fix: Align the Assertion

```java
verify(hashOperations).put(eq("cart:" + userId), eq(productId), contains("\"quantity\":5"));
verify(redisTemplate).delete("cart:guest:" + anonId);  // was: hashOperations.delete
```

## The Result

```
[INFO] Reactor Summary for ecommerce-platform 1.0.0:
[INFO] ecommerce-platform ................................. SUCCESS
[INFO] BUILD SUCCESS
```

All **17 modules** compile and test on Java 17, with **0 failures** across hundreds of tests (a handful `@Disabled` by design).

---

## Key Takeaways

1. **Mismatch between compile-time and test-time JDK is silent until Surefire forks** — toolchains make the build self-describing and portable.
2. **`@Builder` alone breaks Jackson deserialization** — always pair it with `@NoArgsConstructor` + `@AllArgsConstructor` for DTOs.
3. **A failing test isn't always a broken implementation** — verify which mock method your production code actually calls before "fixing" the code.

Have you hit a JDK-toolchain or Lombok/Jackson gotcha in a monorepo? Drop it in the comments. 👇

---

*Part of a 17-service Spring Boot e-commerce platform: auth, user, product, category, cart, order, payment, inventory, coupon, notification, review, elastic search, admin, API gateway, and Eureka service discovery.*
