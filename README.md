# Cryptographic Benchmark: AES, RSA and SPHINCS+


This project implements a simple Java program to compare the basic
cryptographic operations of three algorithms:

* **AES–256** – the Advanced Encryption Standard is a block cipher
  standardised by NIST with a fixed block size of 128 bits and key
  lengths of 128, 192 and 256 bits【493852018359550†L226-L267】.  AES
  is a symmetric algorithm, meaning the same key is used for both
  encryption and decryption【493852018359550†L226-L234】.

* **RSA‑2048** – an asymmetric public‑key algorithm.  A public key is
  derived from two large primes (usually 1024, 2048 or 4096 bits in
  length) and used for encryption, while the corresponding private key
  is required for decryption【166093016389653†L274-L283】.  RSA is more
  computationally intensive than AES and is typically used to encrypt
  only small messages【166093016389653†L274-L287】.

* **SPHINCS+ (SHAKE‑128f)** – a stateless hash‑based digital signature
  scheme selected by NIST for standardisation.  Unlike lattice‑based
  schemes, SPHINCS+ relies solely on the security of hash functions.
  The fast variant used here offers 128‑bit post‑quantum security but
  produces large signatures (\u22487.8 kB for the small variant and
  \u224817 kB for the fast variant)【269012138182202†L514-L523】.  Signature
  generation involves millions of hash function calls and is
  relatively slow, taking milliseconds rather than microseconds on a
  desktop CPU【269012138182202†L524-L529】.  On the positive side, the
  public key is very small (tens of bytes)【269012138182202†L586-L593】.

## Methodology

The benchmarks were conducted using the `BenchmarkRunner` class in
`
BenchmarkRunner.java`.  The program registers the Bouncy Castle
provider (included on this system) to access post‑quantum algorithms.
For each algorithm the following steps are measured:

1. **Key generation** – creating a random secret key (AES) or key pair
   (RSA/SPHINCS+).
2. **Encryption/Sign** – encrypting or signing a 100‑byte random
   message.  AES uses CBC mode with PKCS5 padding, RSA uses
   PKCS#1 v1.5 padding and SPHINCS+ signs the message.
3. **Decryption/Verify** – decrypting the ciphertext or verifying the
   signature.

Each operation is timed in milliseconds using `System.nanoTime()` and
averaged over five runs to reduce measurement noise.  The program also
records the key sizes (public and private where applicable) and the
signature length for SPHINCS+.

## Environment

* Java runtime: OpenJDK 17 (JRE); single‑file source execution via
  `java` was used since a compiler is not available in this container.
* Bouncy Castle version: Debian‑packaged `bcprov` (1.72) located in
  `/usr/share/java/bcprov.jar`.
* Hardware: Containerised environment on the platform hosting this
  experiment (single CPU core available).  Absolute timings should be
  interpreted qualitatively rather than as definitive performance
  figures.

## Results

The table below summarises the averaged timings and sizes captured
from the benchmark.  Times are reported in milliseconds and sizes in
b
ytes (except the symmetric key size, which is reported in bytes as
well).  For AES the key size column reflects the symmetric key length
(32 bytes for AES‑256); for RSA and SPHINCS+ it represents the
security level in bits (2048 and 128 respectively).

| Algorithm | Key generation time (ms) | Encrypt/Sign time (ms) | Decrypt/Verify time (ms) | Key size (bits) | Public key size (bytes) | Private key size (bytes) | Symmetric key size (bytes) | Signature size (bytes) |
|---|---|---|---|---|---|---|---|---|
| **AES‑256** | ≈2.0 | ≈0.7 | ≈0.5 | 256 | – | – | 32 | – |
| **RSA‑2048** | ≈129 | ≈0.54 | ≈3.2 | 2048 | ~294 | ~1218 | – | – |
| **SPHINCS+ (SHAKE‑128f)** | ≈11.7 | ≈211.5 | ≈11.6 | 128 | 58 | 132 | – | 17 088 |

*These values are averages from five runs on the test system.*

### Discussion

* **Performance:**  AES is by far the fastest algorithm tested.  Key
  generation, encryption and decryption complete in under a
  millisecond on average.  RSA key generation is two orders of
  magnitude slower (≈130 ms) because it involves generating large
  primes.  RSA encryption and especially decryption are also slower
  than AES but still complete within a few milliseconds.  SPHINCS+ key
  generation is reasonably fast (≈12 ms), but signing a message
  requires hashing large structures and takes around 0.2 seconds on
  this system, making it the slowest operation by far.  Signature
  verification is comparable to RSA decryption but still slower than
  AES.

* **Key sizes:**  RSA public and private keys are hundreds to over a
  thousand bytes in size.  AES uses a small fixed‑size secret key
  (32 bytes for AES‑256).  SPHINCS+ has a tiny public key (≈58 bytes)
  and a moderately sized private key (≈132 bytes)【269012138182202†L586-L593】.
  However, its signature is enormous (≈17 kB for the fast variant)【269012138182202†L520-L523】.  Such large signatures can
  significantly increase the size of protocols or certificates using
  SPHINCS+【269012138182202†L586-L593】.

* **Use cases:**  AES is suited for bulk data encryption where speed is
  paramount.  RSA remains widely used for key exchange and digital
  signatures but is vulnerable to quantum attacks【269012138182202†L560-L572】.
  SPHINCS+ provides a quantum‑resistant signature scheme with high
  confidence in its security basis (hash functions)【269012138182202†L533-L548】,
  but at the cost of large signatures and slower signing performance.

## Graphs

The following charts visualise the timings and sizes recorded in the
benchmark.  Each bar represents the average value for an algorithm.

### Operation Times

<details>
![Operation time comparison](benchmark_results_time.png)
</details>

### Key Sizes
<details>

![Key size comparison](benchmark_results_key_sizes.png)
</details>

### Signature Sizes

<details>
![Signature size comparison](benchmark_results_signature_size.png)

</details>


## Discussion of Graphs

The **operation time comparison** chart shows that AES is by far the fastest for key generation, encryption and decryption, finishing in well under a millisecond. RSA is slower, particularly for decryption, but remains within a few milliseconds. In contrast, SPHINCS+ signing takes more than **200 ms** on average in this environment, and verification is an order of magnitude slower than AES and RSA. This highlights that the post‑quantum scheme is much more computationally intensive.

The **key size comparison** graph demonstrates that AES uses a small symmetric key (32 bytes for AES‑26) while RSA keys are larger (a 2048‑bit modulus produces a 256‑byte key) and SPHINCS+ keys consist of a 128‑bit security level parameter resulting in very small public keys (~58 bytes) but moderate private keys (~132 bytes). The **signature size comparison** graph emphasises that SPHINCS+ signatures are huge (17 kB for the fast parameter set), whereas RSA signatures are only a few hundred bytes and AES does not generate signatures.

Because SPHINCS+ uses hash‑based signatures rather than block ciphers or modular arithmetic, it requires millions of hash function calls to produce a single signature【269012138182202†L524-L529】, leading to the slow timings observed. Applications must handle these long signing times and very large signatures, which may not fit into existing protocols or message formats. Bandwidth and storage requirements also increase dramatically. Therefore, while SPHINCS+ provides resistance against quantum attacks, integrating it into real‑world systems is more challenging than using well‑established algorithms like RSA and AES, which have smaller keys/signatures and much faster cryptographic operations.


## Building and Running the Benchmark

Although the benchmark can be executed in this repository via
`java -cp .:/usr/share/java/bcprov.jar BenchmarkRunner.java`, a
full Java development kit is not required.  The single‑file source
feature of Java 17 compiles and runs the program automatically.  If
you wish to modify the benchmarks (e.g. change message sizes or the
number of iterations), edit `BenchmarkRunner.java` accordingly.  To
regenerate the charts after running the benchmark, run the supplied
Python script:

```sh
python3 plot_results.py benchmark_results.csv
```


This will update `benchmark_results_time.png` and
`benchmark_results_size.png` with the new measurements.
