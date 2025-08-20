import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.spec.SPHINCSPlusParameterSpec;
import java.security.Signature;

/**
 * BenchmarkRunner exercises the AES, RSA and SPHINCS+ cryptographic
 * primitives using the standard Java APIs together with the BouncyCastle
 * provider for post‑quantum support. For each algorithm the program
 * measures the time taken to generate a keypair (where applicable),
 * encrypt/sign a short message and decrypt/verify the resulting
 * ciphertext/signature. The results are printed to standard output as
 * comma‑separated values so they can be processed further by a script
 * or spreadsheet.
 */
public class BenchmarkRunner {

    /**
     * Container class to hold the benchmark statistics for each
     * algorithm. Times are measured in milliseconds.  Key sizes are
     * reported in bits for asymmetric keys and bytes for symmetric
     * keys. Signature sizes are given in bytes.
     */
    static class Result {
        String algorithm;
        double keyGenTimeMs;
        double encryptTimeMs;
        double decryptTimeMs;
        int keySizeBits;
        int publicKeySizeBytes;
        int privateKeySizeBytes;
        int symmetricKeySizeBytes;
        int signatureSizeBytes;
    }

    public static void main(String[] args) throws Exception {
        // Register BouncyCastle as a security provider so that
        // SPHINCS+ algorithms become available via JCA.
        Security.addProvider(new BouncyCastleProvider());

        int iterations = 5; // number of benchmark runs to average
        // run AES with a 256‑bit key
        Result aesRes = runAES(256, iterations);
        // run RSA with a 2048‑bit modulus
        Result rsaRes = runRSA(2048, iterations);
        // run SPHINCS+ using the SHAKE‑128f parameter set (fast variant)
        Result sphincsRes = runSphincs(iterations);

        // Print header
        System.out.println("algorithm,keyGenTimeMs,encryptOrSignTimeMs,decryptOrVerifyTimeMs,keySizeBits,publicKeyBytes,privateKeyBytes,symmetricKeyBytes,signatureBytes");
        // Print results
        printResult(aesRes);
        printResult(rsaRes);
        printResult(sphincsRes);
    }

    private static void printResult(Result r) {
        System.out.printf("%s,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d%n",
                r.algorithm,
                r.keyGenTimeMs,
                r.encryptTimeMs,
                r.decryptTimeMs,
                r.keySizeBits,
                r.publicKeySizeBytes,
                r.privateKeySizeBytes,
                r.symmetricKeySizeBytes,
                r.signatureSizeBytes);
    }

    /**
     * Benchmarks AES encryption/decryption using the specified key length.
     * AES is a symmetric cipher so there is no public/private keypair. A
     * random 100‑byte message is encrypted and decrypted once per
     * iteration. Because AES is extremely fast, the measured times may
     * be very small; averaging over several runs reduces variance.
     *
     * @param keySize key length in bits (e.g. 128 or 256)
     * @param iterations number of runs to average over
     * @return the populated Result instance
     * @throws Exception on cryptographic errors
     */
    public static Result runAES(int keySize, int iterations) throws Exception {
        Result result = new Result();
        result.algorithm = "AES";
        result.keySizeBits = keySize;
        result.publicKeySizeBytes = 0;
        result.privateKeySizeBytes = 0;
        result.signatureSizeBytes = 0;
        result.symmetricKeySizeBytes = keySize / 8;

        SecureRandom random = new SecureRandom();
        byte[] plain = new byte[100];
        random.nextBytes(plain);

        double totalKeyGen = 0;
        double totalEncrypt = 0;
        double totalDecrypt = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize);
            SecretKey secretKey = keyGen.generateKey();
            long mid = System.nanoTime();

            // Use AES/CBC/PKCS5Padding with a random IV
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] cipherText = cipher.doFinal(plain);
            long encEnd = System.nanoTime();

            // Decrypt
            Cipher decipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] recovered = decipher.doFinal(cipherText);
            long end = System.nanoTime();

            totalKeyGen += (mid - start) / 1e6;
            totalEncrypt += (encEnd - mid) / 1e6;
            totalDecrypt += (end - encEnd) / 1e6;
            // simple sanity check
            if (!java.util.Arrays.equals(plain, recovered)) {
                throw new IllegalStateException("AES decryption failed");
            }
        }
        result.keyGenTimeMs = totalKeyGen / iterations;
        result.encryptTimeMs = totalEncrypt / iterations;
        result.decryptTimeMs = totalDecrypt / iterations;
        return result;
    }

    /**
     * Benchmarks RSA encryption/decryption for a given modulus size. A
     * 100‑byte message is encrypted and decrypted once per iteration.
     * RSA encryption has a limitation on the size of plaintext it can
     * encrypt based on the key size and padding; a 2048‑bit key with
     * PKCS#1 padding can encrypt up to 245 bytes, so 100 bytes is safe.
     *
     * @param keySize modulus length in bits (e.g. 2048)
     * @param iterations number of runs to average over
     * @return the populated Result
     * @throws Exception on cryptographic errors
     */
    public static Result runRSA(int keySize, int iterations) throws Exception {
        Result result = new Result();
        result.algorithm = "RSA";
        result.keySizeBits = keySize;
        result.signatureSizeBytes = 0;
        result.symmetricKeySizeBytes = 0;

        SecureRandom random = new SecureRandom();
        byte[] plain = new byte[100];
        random.nextBytes(plain);

        double totalKeyGen = 0;
        double totalEncrypt = 0;
        double totalDecrypt = 0;
        int publicKeyLength = 0;
        int privateKeyLength = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keySize);
            KeyPair kp = kpg.generateKeyPair();
            long mid = System.nanoTime();

            PublicKey publicKey = kp.getPublic();
            PrivateKey privateKey = kp.getPrivate();
            // update key size metrics only once
            if (i == 0) {
                publicKeyLength = publicKey.getEncoded().length;
                privateKeyLength = privateKey.getEncoded().length;
            }
            // Encrypt using RSA/ECB/PKCS1Padding
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherText = cipher.doFinal(plain);
            long encEnd = System.nanoTime();
            // Decrypt
            Cipher decipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] recovered = decipher.doFinal(cipherText);
            long end = System.nanoTime();
            totalKeyGen += (mid - start) / 1e6;
            totalEncrypt += (encEnd - mid) / 1e6;
            totalDecrypt += (end - encEnd) / 1e6;
            if (!java.util.Arrays.equals(plain, recovered)) {
                throw new IllegalStateException("RSA decryption failed");
            }
        }
        result.keyGenTimeMs = totalKeyGen / iterations;
        result.encryptTimeMs = totalEncrypt / iterations;
        result.decryptTimeMs = totalDecrypt / iterations;
        result.publicKeySizeBytes = publicKeyLength;
        result.privateKeySizeBytes = privateKeyLength;
        return result;
    }

    /**
     * Benchmarks SPHINCS+ key generation, signing and verification using
     * the SHAKE‑128f parameter set (fast variant). A 100‑byte random
     * message is signed and the signature is verified once per iteration.
     *
     * @param iterations number of runs to average over
     * @return the populated Result
     * @throws Exception on cryptographic errors
     */
    public static Result runSphincs(int iterations) throws Exception {
        Result result = new Result();
        result.algorithm = "SPHINCS+";
        result.symmetricKeySizeBytes = 0;

        SecureRandom random = new SecureRandom();
        byte[] message = new byte[100];
        random.nextBytes(message);

        double totalKeyGen = 0;
        double totalSign = 0;
        double totalVerify = 0;
        int publicKeyLength = 0;
        int privateKeyLength = 0;
        int signatureLength = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            // Obtain a key pair generator for SPHINCS+
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("SPHINCSPLUS", "BC");
            // Use the SHAKE‑128f parameter set: fast variant at 128‑bit security
            kpg.initialize(SPHINCSPlusParameterSpec.shake_128f, random);
            KeyPair kp = kpg.generateKeyPair();
            long mid = System.nanoTime();

            PublicKey publicKey = kp.getPublic();
            PrivateKey privateKey = kp.getPrivate();
            if (i == 0) {
                publicKeyLength = publicKey.getEncoded().length;
                privateKeyLength = privateKey.getEncoded().length;
            }
            // Sign
            Signature sphincsSigner = Signature.getInstance("SPHINCSPLUS", "BC");
            sphincsSigner.initSign(privateKey, random);
            sphincsSigner.update(message);
            byte[] signature = sphincsSigner.sign();
            long signEnd = System.nanoTime();
            // Save signature length from first run
            if (i == 0) {
                signatureLength = signature.length;
            }
            // Verify
            Signature sphincsVerifier = Signature.getInstance("SPHINCSPLUS", "BC");
            sphincsVerifier.initVerify(publicKey);
            sphincsVerifier.update(message);
            boolean ok = sphincsVerifier.verify(signature);
            long end = System.nanoTime();
            if (!ok) {
                throw new IllegalStateException("SPHINCS+ verification failed");
            }
            totalKeyGen += (mid - start) / 1e6;
            totalSign += (signEnd - mid) / 1e6;
            totalVerify += (end - signEnd) / 1e6;
        }
        result.keyGenTimeMs = totalKeyGen / iterations;
        result.encryptTimeMs = totalSign / iterations;
        result.decryptTimeMs = totalVerify / iterations;
        result.publicKeySizeBytes = publicKeyLength;
        result.privateKeySizeBytes = privateKeyLength;
        result.signatureSizeBytes = signatureLength;
        // For SPHINCS+ the 'key size bits' refers to the security level (128)
        result.keySizeBits = 128;
        return result;
    }
}
