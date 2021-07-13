package com.stefan.disertation;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class Block {

    private final int index;
    private static AtomicInteger a = new AtomicInteger();
    private String hash;
    private final String prevHash;
    private String merkleRoot;
    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private final long timeStamp;
    private int nonce;

    public Block(String prevHash) {
        this.index = a.getAndIncrement();
        this.prevHash = prevHash;
        this.timeStamp = index == 0 ? genesis() : new Date().getTime();
        this.hash = computeHash();
    }

    public static void updateIndex(int count) {
        System.out.println("Index is " + a.get() + ", updating...");
        a = new AtomicInteger(count);
        System.out.println("Index updated to " + a.get());
    }
    
    public int getIndex() {
    	return this.index;
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    private long genesis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1997);
        c.set(Calendar.DATE, 11);
        c.set(Calendar.DATE, 23);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date d = c.getTime();
        return d.getTime();
    }

    public String getHash() {
        return this.hash;
    }

    public String getPrevHash() {
        return this.prevHash;
    }

    public String computeHash() {
        return CryptoUtil.sha256(prevHash + index + timeStamp + nonce + merkleRoot);
    }

    public void mineBlock(int difficulty) {
        merkleRoot = CryptoUtil.merkleRoot(transactions);
        String target = CryptoUtil.getDifficulty(difficulty);
        System.out.println("Trying to mine block " + index + " ...");
        while(!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = computeHash();
        }
        System.out.println("Block mined -> " + hash);
    }

    public boolean addTransaction(Transaction t) {
        if ((t == null) || (!"0".equals(prevHash) && !t.process())) {
            return false;
        }
        transactions.add(t);
        return true;
    }
}
