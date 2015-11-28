/*
 * All Rights Reserved
 */
package io.github.tommsy64.satchels.util;

import java.security.SecureRandom;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

/**
 * Creator: BouncyCastle 
 * Modifier: LordLambda, Tommsy64
 * Date: 5/3/15 
 * Usage: Implementation of the RC5 algorithm.
 */
public class RC5 {

  private static final int wordSize = 32;
  private static final int bytesPerWord = wordSize / 8;

  /*
   * the number of rounds to perform
   */
  private static final int _noRounds = 20;

  /*
   * the expanded key array of size 2*(rounds + 1)
   */
  private int _S[];

  public byte baseKey[];

  private static final int P32 = 0xb7e15163;
  private static final int Q32 = 0x9e3779b9;

  private static final int LGW = 5; // log2(32)

  public int getBlockSize() {
    return 4 * bytesPerWord;
  }

  private static byte[] genKey() {
    byte key[] = new byte[192];
    SecureRandom sr = new SecureRandom();
    sr.nextBytes(key);
    return key;
  }

  public RC5() throws Exception {
    this(genKey());
  }

  public RC5(String key) throws Exception {
    this(key.getBytes());
  }

  public RC5(byte[] key) throws Exception {
    baseKey = Base64Util.encode(key).getBytes();
    setKey(key);
  }

  private void setKey(byte[] key) {
    //
    // KEY EXPANSION:
    //
    // There are 3 phases to the key expansion.
    //
    // Phase 1:
    // Copy the secret key K[0...b-1] into an array L[0..c-1] of
    // c = ceil(b/u), where u = wordSize/8 in little-endian order.
    // In other words, we fill up L using u consecutive key bytes
    // of K. Any unfilled byte positions in L are zeroed. In the
    // case that b = c = 0, set c = 1 and L[0] = 0.
    //
    // compute number of dwords
    int c = (key.length + (bytesPerWord - 1)) / bytesPerWord;
    if (c == 0) {
      c = 1;
    }
    int[] L = new int[(key.length + bytesPerWord - 1) / bytesPerWord];

    // load all key bytes into array of key dwords
    for (int i = key.length - 1; i >= 0; i--) {
      L[i / bytesPerWord] = (L[i / bytesPerWord] << 8) + (key[i] & 0xff);
    }

    //
    // Phase 2:
    // Key schedule is placed in a array of 2+2*ROUNDS+2 = 44 dwords.
    // Initialize S to a particular fixed pseudo-random bit pattern
    // using an arithmetic progression modulo 2^wordsize determined
    // by the magic numbers, Pw & Qw.
    //
    _S = new int[2 + 2 * _noRounds + 2];

    _S[0] = P32;
    for (int i = 1; i < _S.length; i++) {
      _S[i] = (_S[i - 1] + Q32);
    }

    //
    // Phase 3:
    // Mix in the user's secret key in 3 passes over the arrays S & L.
    // The max of the arrays sizes is used as the loop control
    //
    int iter;

    if (L.length > _S.length) {
      iter = 3 * L.length;
    } else {
      iter = 3 * _S.length;
    }

    int A = 0;
    int B = 0;
    int i = 0, j = 0;

    for (int k = 0; k < iter; k++) {
      A = _S[i] = rotateLeft(_S[i] + A + B, 3);
      B = L[j] = rotateLeft(L[j] + A + B, A + B);
      i = (i + 1) % _S.length;
      j = (j + 1) % L.length;
    }
  }

  /**
   * Perform a left "spin" of the word. The rotation of the given word <em>x</em> is rotated left by <em>y</em> bits. Only the <em>lg(wordSize)</em> low-order
   * bits of <em>y</em> are used to determine the rotation amount. Here it is assumed that the wordsize used is 32.
   * <p>
   * 
   * @param x
   *            word to rotate
   * @param y
   *            number of bits to rotate % wordSize
   */
  private int rotateLeft(int x, int y) {
    return (x << y) | (x >>> -y);
  }

  /**
   * Perform a right "spin" of the word. The rotation of the given word <em>x</em> is rotated left by <em>y</em> bits. Only the <em>lg(wordSize)</em>
   * low-order bits of <em>y</em> are used to determine the rotation amount. Here it is assumed that the wordsize used is a power of 2.
   * <p>
   * 
   * @param x
   *            word to rotate
   * @param y
   *            number of bits to rotate % wordSize
   */
  private int rotateRight(int x, int y) {
    return (x >>> y) | (x << -y);
  }

  private int bytesToWord(byte[] src, int srcOff) {
    int word = 0;

    for (int i = bytesPerWord - 1; i >= 0; i--) {
      word = (word << 8) + (src[i + srcOff] & 0xff);
    }

    return word;
  }

  private void wordToBytes(int word, byte[] dst, int dstOff) {
    for (int i = 0; i < bytesPerWord; i++) {
      dst[i + dstOff] = (byte) word;
      word >>>= 8;
    }
  }

  public int processBlock(byte[] in, int inOff, byte[] out, int outOff, boolean forEncryption) throws Exception {
    int blockSize = getBlockSize();
    if (_S == null) {
      throw new IllegalStateException("RC5 engine not initialised");
    }
    if ((inOff + blockSize) > in.length) {
      throw new Exception("input buffer too short");
    }
    if ((outOff + blockSize) > out.length) {
      throw new Exception("output buffer too short");
    }

    return (forEncryption) ? encryptBlock(in, inOff, out, outOff) : decryptBlock(in, inOff, out, outOff);
  }

  private int encryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
    // load A,B,C and D registers from in.
    int A = bytesToWord(in, inOff);
    int B = bytesToWord(in, inOff + bytesPerWord);
    int C = bytesToWord(in, inOff + bytesPerWord * 2);
    int D = bytesToWord(in, inOff + bytesPerWord * 3);

    // Do pseudo-round #0: pre-whitening of B and D
    B += _S[0];
    D += _S[1];

    // perform round #1,#2 ... #ROUNDS of encryption
    for (int i = 1; i <= _noRounds; i++) {
      int t = 0, u = 0;

      t = B * (2 * B + 1);
      t = rotateLeft(t, 5);

      u = D * (2 * D + 1);
      u = rotateLeft(u, 5);

      A ^= t;
      A = rotateLeft(A, u);
      A += _S[2 * i];

      C ^= u;
      C = rotateLeft(C, t);
      C += _S[2 * i + 1];

      int temp = A;
      A = B;
      B = C;
      C = D;
      D = temp;
    }
    // do pseudo-round #(ROUNDS+1) : post-whitening of A and C
    A += _S[2 * _noRounds + 2];
    C += _S[2 * _noRounds + 3];

    // store A, B, C and D registers to out
    wordToBytes(A, out, outOff);
    wordToBytes(B, out, outOff + bytesPerWord);
    wordToBytes(C, out, outOff + bytesPerWord * 2);
    wordToBytes(D, out, outOff + bytesPerWord * 3);

    return 4 * bytesPerWord;
  }

  private int decryptBlock(byte[] in, int inOff, byte[] out, int outOff) {
    // load A,B,C and D registers from out.
    int A = bytesToWord(in, inOff);
    int B = bytesToWord(in, inOff + bytesPerWord);
    int C = bytesToWord(in, inOff + bytesPerWord * 2);
    int D = bytesToWord(in, inOff + bytesPerWord * 3);

    // Undo pseudo-round #(ROUNDS+1) : post whitening of A and C
    C -= _S[2 * _noRounds + 3];
    A -= _S[2 * _noRounds + 2];

    // Undo round #ROUNDS, .., #2,#1 of encryption
    for (int i = _noRounds; i >= 1; i--) {
      int t = 0, u = 0;

      int temp = D;
      D = C;
      C = B;
      B = A;
      A = temp;

      t = B * (2 * B + 1);
      t = rotateLeft(t, LGW);

      u = D * (2 * D + 1);
      u = rotateLeft(u, LGW);

      C -= _S[2 * i + 1];
      C = rotateRight(C, t);
      C ^= u;

      A -= _S[2 * i];
      A = rotateRight(A, u);
      A ^= t;

    }
    // Undo pseudo-round #0: pre-whitening of B and D
    D -= _S[1];
    B -= _S[0];

    wordToBytes(A, out, outOff);
    wordToBytes(B, out, outOff + bytesPerWord);
    wordToBytes(C, out, outOff + bytesPerWord * 2);
    wordToBytes(D, out, outOff + bytesPerWord * 3);

    return 4 * bytesPerWord;
  }

  public byte[] encrypt(byte[] data) {
    while (data.length % 16 != 0) {
      data = Arrays.copyOf(data, data.length + 1);
    }
    byte[] retV = new byte[0];
    byte[][] split = new byte[(data.length / 16)][16];
    for (int i = 0, j = 0; j < (data.length / 16); i += 16, j++) {
      split[j] = Arrays.copyOfRange(data, i, i + 16);
    }
    for (byte[] b : split) {
      byte[] out = new byte[16];
      encryptBlock(b, 0, out, 0);
      retV = ArrayUtils.addAll(retV, out);
    }
    return retV;
  }

  public byte[] decrypt(byte[] data) throws Exception {
    if (!(data.length % 16 == 0)) {
      throw new Exception("Invalid Block Size");
    }
    byte[] retV = new byte[0];
    byte[][] split = new byte[(data.length / 16)][16];
    for (int i = 0, j = 0; j < (data.length / 16); i += 16, j++) {
      split[j] = Arrays.copyOfRange(data, i, i + 16);
    }
    for (byte[] b : split) {
      byte[] out = new byte[16];
      decryptBlock(b, 0, out, 0);
      retV = ArrayUtils.addAll(retV, out);
    }
    return retV;
  }
}
