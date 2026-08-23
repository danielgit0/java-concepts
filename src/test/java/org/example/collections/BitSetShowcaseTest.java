package org.example.collections;

import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BitSetShowcaseTest {

  @Test
  public void testBitSet() {
    System.out.println("\n--- BITSET EXAMPLE ---");

    // PROBLEM: Track which of 70 million users have completed Known Your Customer (KYC)
    // verification.
    // Hint: Boolean[] would use 70M bytes (~70 MB).

    // SOLUTION: BitSet for memory-efficient flags. BitSet uses ~8 MB.
    BitSet kycVerified = new BitSet(70_000_000);

    // Mark users as verified (simulate 10 million users)
    Random random = new Random(123);
    for (int i = 0; i < 10_000_000; i++) {
      int userId = random.nextInt(70_000_000);
      kycVerified.set(userId);
    }

    // Check verification status
    int testUser = 12345;
    System.out.println("User " + testUser + " KYC verified: " + kycVerified.get(testUser));

    // Count verified users efficiently
    int verifiedCount = kycVerified.cardinality();
    System.out.println("Total verified users: " + verifiedCount);

    System.out.println("Memory used (approx): " + (kycVerified.size() / 8 / 1024 / 1024) + " MB");

    // To return unset/clear bits it is possible to use: kycVerified.nextClearBit();
  }
}
