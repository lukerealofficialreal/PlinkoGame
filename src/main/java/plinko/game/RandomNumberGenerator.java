package main.java.plinko.game;

import java.nio.ByteBuffer;

public class RandomNumberGenerator{
    private Long key;

    public RandomNumberGenerator(long seed) {
        this.key = advanceKey(seed);
    }

    public RandomNumberGenerator(RandomNumberGenerator other) {
        this.key = other.key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public Long getKey() {
        return key;
    }

    public boolean nextBoolean() {
        boolean val = (key%2 == 0);
        key = advanceKey(key);
        return val;
    }

    public long nextLong() {
        long val = key;
        key = advanceKey(key);
        return val;
    }

    public int nextInt() {
        int val = key.intValue();
        key = advanceKey(key);
        return val;
    }

    public byte[] encrypt(byte[] data) {

        //Convert both to byte[]
        byte[] keyBytes = longToBytes(key);
        byte[] cryptedMsg = new byte[data.length];

        for(int i = 0; i < data.length; i++) {
            cryptedMsg[i] = (byte) (data[i] ^ keyBytes[i%keyBytes.length]);
        }

        return cryptedMsg;
    }

    //Passes through if key is null
    public byte[] decrypt(byte[] data) {
        return encrypt(data);
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private static long advanceKey(long r)
    {
        r ^= r << 13; r ^= r >>> 7; r ^= r << 17; return r;
    }
}
