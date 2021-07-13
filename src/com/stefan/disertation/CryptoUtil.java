package com.stefan.disertation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

public class CryptoUtil {

    private static final Gson gson = new Gson();

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String merkleRoot(ArrayList<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> oldLayer = new ArrayList<>();
        for(Transaction t : transactions) {
            oldLayer.add(t.getID());
        }
        ArrayList<String> newLayer = oldLayer;
        while (count > 1) {
            newLayer = new ArrayList<>();
            for(int i = 1; i < oldLayer.size(); i++) {
                newLayer.add(sha256(oldLayer.get(i-1) + oldLayer.get(i)));
            }
            count = newLayer.size();
            oldLayer = newLayer;
        }
        return (newLayer.size() == 1) ? newLayer.get(0) : "";
    }

    public static String getJson(Object obj) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
    }

    public static String getDifficulty(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    public static ArrayList<Block> getChainFromGson(String json) {
        Block[] blockArray = gson.fromJson(json, Block[].class);
        ArrayList<Block> chain = new ArrayList<>();
        Collections.addAll(chain, blockArray);
        return chain;
    }

    public static LinkedHashSet<String> getNodesFromGson(String json) {
        String[] nodeArray = gson.fromJson(json, String[].class);
        ArrayList<String> nodes = new ArrayList<>();
        Collections.addAll(nodes, nodeArray);
        return new LinkedHashSet<>(nodes);
    }

    public static Block getBlockFromGson(String json) {
        return gson.fromJson(json, Block.class);
    }

    public static String getHostFromGson(String host) {
        return gson.fromJson(host, String.class);
    }

}
