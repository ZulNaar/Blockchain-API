package com.stefan.disertation;
import java.util.concurrent.atomic.AtomicInteger;

public class Transaction {

    private String id = "0";
    private final String data;
    private static AtomicInteger sequence = new AtomicInteger(1);

    public Transaction(String data) {
        this.data = data;
    }

    public String getID() {
        return this.id;
    }

    public static void setSequence(int value) {
        System.out.println("Sequence is " + sequence.get() + ", updating...");
        sequence = new AtomicInteger(value);
        System.out.println("Sequence updated to " + sequence.get());
    }

    private String hash() {
        return CryptoUtil.sha256(data + sequence.getAndIncrement());
    }

    public boolean process() {
        id = hash();
        //validation
        return true;
    }
}
