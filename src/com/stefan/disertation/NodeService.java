package com.stefan.disertation;
import java.util.LinkedHashSet;

import static spark.Spark.*;

public class NodeService {

    public static LinkedHashSet<String> nodes = new LinkedHashSet<>();

    public static void main(String[] args) {
        port(23532);
        post("/register", (req, res) -> {
        	registerNode(CryptoUtil.getHostFromGson(req.body()));
            res.type("application/json");
            return "Node added";
        });
        get("/nodes", (req, res) -> {
            res.type("application/json");
            return CryptoUtil.getJson(nodes);
        });
    }
    public static void registerNode(String host) {
        nodes.add(host);
        System.out.println("Added node: " + host);
    }
}
