package com.stefan.disertation;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class BlockChain {
    private static Block shadowBlock;
    public static ArrayList<Block> chain = new ArrayList<>();
    public static LinkedHashSet<String> nodes = new LinkedHashSet<>();
    private static int difficulty = 4;
    private static String port;

    private BlockChain(int port) {
        System.out.println("Initializing...");
        BlockChain.port = "localhost:" + port;
        register();
        Transaction genesisTransaction = new Transaction("In the beginning there was the Genesis Block");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        shadowBlock = genesis;
        mineBlock();
    }

    public static BlockChain init(int port) {
        updateNodes();
        return new BlockChain(port);
    }

    public Boolean isValid() {
        Block currBlock;
        Block prevBlock;
        String hashTarget = CryptoUtil.getDifficulty(difficulty);

        for (int i = 1; i < chain.size(); i++) {
            currBlock = chain.get(i);
            prevBlock = chain.get(i-1);

            if(!currBlock.getHash().equals(currBlock.computeHash())){
                System.out.println("Current hash mismatch!");
                return false;
            }
            if(!prevBlock.getHash().equals(currBlock.getPrevHash())){
                System.out.println("Previous hash mismatch!");
                return false;
            }
            if(!currBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                System.out.println("Block not mined!");
                return false;
            }
        }
        return true;
    }

    private void register() {
        try {
            System.out.println("Registering on the Node Service...");
            URL url = new URL("http://localhost:23532/register");
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] out = CryptoUtil.getJson(port).getBytes();
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
            http.disconnect();
            System.out.println("Registration complete!");
        } catch (Exception e) {
            System.out.println("Error contacting the Node Service");
        }
    }

    private Boolean isValid(ArrayList<Block> otherChain) {
        Block currBlock;
        Block prevBlock;
        String hashTarget = CryptoUtil.getDifficulty(difficulty);

        for (int i = 1; i < otherChain.size(); i++) {
            currBlock = otherChain.get(i);
            prevBlock = otherChain.get(i-1);

            if(!currBlock.getHash().equals(currBlock.computeHash())){
                System.out.println("Current hash mismatch!");
                return false;
            }
            if(!prevBlock.getHash().equals(currBlock.getPrevHash())){
                System.out.println("Previous hash mismatch!");
                return false;
            }
            if(!currBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                System.out.println("Block not mined!");
                return false;
            }
        }
        return true;
    }

    public void addTransaction(String data) {
        if(shadowBlock == null) shadowBlock = new Block(getLast().getHash());
        Transaction t = new Transaction(data);
        if(shadowBlock.addTransaction(t)){
            System.out.println("Transaction successfully added!");
        }
        else{
            System.out.println("Transaction failed to process. Discarding...");
        }
    }

    public void appendBlock(Block newBlock) {
        System.out.println("Validating incoming block...");
        if(newBlock.getHash().equals(newBlock.computeHash()) && chain.get(chain.size()-1).getHash().equals(newBlock.getPrevHash()) && newBlock.getHash().substring(0, difficulty).equals(CryptoUtil.getDifficulty(difficulty))) {
            chain.add(newBlock);
            Block.updateIndex(chain.size());
            Transaction.setSequence(getTransactionCount());
            //TODO: update transactions in the shadowblock
            System.out.println("Blockchain updated!");
        }
        else {
            System.out.println("The block is invalid! Checking if node is out of sync...");
            resolveConflicts();
        }
    }

    public void mineBlock() {
        if(shadowBlock == null) shadowBlock = new Block(getLast().getHash());
        shadowBlock.mineBlock(difficulty);
        chain.add(shadowBlock);
        shadowBlock=null;
        //broadcast to other nodes
        if(chain.size() > 1)
            broadcastMining();
    }

    public Block getLast() {
        return chain.get(chain.size()-1);
    }

    public String toJson() {
        return CryptoUtil.getJson(chain);
    }

    public void registerNode(String host) {
        nodes.add(host);
        System.out.println("Added node: " + host);
    }

    public boolean resolveConflicts() {
        updateNodes();
        ArrayList<Block> newChain = null;
        int length = chain.size();
        for(String node : nodes) {
            if(!port.equals(node)) {
                try {
                	URL url = new URL("http://" + node + "/lastblock");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    if(con.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();
                        while ((inputLine = br.readLine()) != null)
                            content.append(inputLine);
                        br.close();
                        con.disconnect();
                        Block otherBlock = CryptoUtil.getBlockFromGson(content.toString());
                        content.setLength(0);
                        if (!otherBlock.getHash().equals(getLast().getHash())) {
                        	if(getLast().getIndex() < otherBlock.getIndex()) {
                        		url = new URL("http://" + node + "/blockchain");
                                con = (HttpURLConnection) url.openConnection();
                                con.setRequestMethod("GET");
                                if(con.getResponseCode() == 200) {
                                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                    while ((inputLine = br.readLine()) != null)
                                        content.append(inputLine);
                                    br.close();
                                    con.disconnect();
                                    ArrayList<Block> otherChain = CryptoUtil.getChainFromGson(content.toString());
                                    //calc len and replace
                                    int otherLength = otherChain.size();
                                    if(isValid(otherChain) && otherLength > length){
                                        length = otherLength;
                                        newChain = otherChain;
                                    }
                                }
                        	}
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error contacting the node " + node);
                }
            }
        }
        if (newChain != null) {
            chain = newChain;
            Block.updateIndex(newChain.size());
            Transaction.setSequence(getTransactionCount());
            System.out.println("Replacing chain");
            return true;
        }
        System.out.println("Chain stays the same");
        return false;
    }

    private int getTransactionCount() {
        int count = 0;
        for (Block b : chain) {
            count += b.getTransactionCount();
        }
        return count;
    }

    private static void updateNodes() {
        try {
            URL url = new URL("http://localhost:23532/nodes");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if(con.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = br.readLine()) != null)
                    content.append(inputLine);
                br.close();
                con.disconnect();
                LinkedHashSet<String> updatedNodes = CryptoUtil.getNodesFromGson(content.toString());
                if (updatedNodes.size() > nodes.size()) {
                    nodes = updatedNodes;
                    System.out.println("Node list updated");
                } else {
                    System.out.println("Node list remains the same");
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Could not connect to the node repository");
        }
    }

    private static void broadcastMining() {
        updateNodes();
        System.out.println("Starting broadcast...");
        for(String node : nodes) {
            if(!port.equals(node)) {
                try {
                    URL url = new URL("http://" + node + "/update");
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection) con;
                    http.setRequestMethod("POST");
                    http.setDoOutput(true);

                    byte[] out = CryptoUtil.getJson(chain.get(chain.size()-1)).getBytes();
                    int length = out.length;

                    http.setFixedLengthStreamingMode(length);
                    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    http.connect();
                    try(OutputStream os = http.getOutputStream()) {
                        os.write(out);
                    }
                    http.disconnect();
                    System.out.println("Broadcast complete for " + node);
                } catch (Exception e) {
                    System.out.println("Error contacting " + node);
                }
            }
        }
    }
}
