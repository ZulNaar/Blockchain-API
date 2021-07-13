package com.stefan.disertation;
import static spark.Spark.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Node {
	
	public static Connection connect() throws SQLException {
		String url = "jdbc:sqlite:bc.db";
		Connection conn = DriverManager.getConnection(url);
		System.out.println("Connection to the database has been established!");
		return conn;
	}

    static BlockChain chain;

    public static void main(String[] args) {
    	
    	int port = 23580;
    	
        port(port);
        
        chain = BlockChain.init(port);

        get("/valid", (req, res) -> chain.isValid());
        get("/blockchain", (req, res) -> {
            res.type("application/json");
            return chain.toJson();
        });
        get("/lastblock", (req, res) -> {
            res.type("application/json");
            return CryptoUtil.getJson(chain.getLast());
        });
        post("/mine", (req, res) -> {
            chain.mineBlock();
            res.type("application/json");
            res.status(201);
            return CryptoUtil.getJson(chain.getLast());
        });
        post("/transaction", (req, res) -> {
        	chain.addTransaction(new Gson().fromJson(req.body(), TransactionHelper.class).data);
            res.status(201);
            return "Transaction added!";
        });
        post("/update", (req, res) -> {
        	chain.appendBlock(CryptoUtil.getBlockFromGson(req.body()));
            res.status(201);
            return "Block added!";
        });
        get("/resolve", (req, res) -> {
            res.type("application/json");
            if(chain.resolveConflicts()) return "Our blockchain was replaced";
            else return "Our blockchain is the main one";
        });
        post("/save", (req, res) -> {
        	Connection conn = null;
        	try {
        		conn = connect();
        		String sql = "create table if not exists BLOCKCHAIN (ID integer primary key autoincrement, CHAIN text not null);";
        		Statement stmt = conn.createStatement();
        		stmt.execute(sql);
        		PreparedStatement ps = conn.prepareStatement("insert into BLOCKCHAIN(CHAIN) values(?)");
        		ps.setString(1, chain.toJson());
        		ps.executeUpdate();
        		System.out.println("Blockchain saved in the database!");
        	} catch (SQLException sqle) {
        		System.out.println(sqle.getMessage());
        	} catch (Exception e) {
        		System.out.println(e.getMessage());
        	} finally {
        		try {
    				if(conn != null) {
    					conn.close();
    					System.out.println("Connection closed!");
    				}
    			} catch (SQLException sqle) {
    				System.out.println(sqle.getMessage());
    			}
        	}
            res.status(201);
            return "Blockchain saved in the database!";
        });
        get("/db", (req, res) -> {
        	String result = "";
        	Connection conn = null;
        	try {
        		conn = connect();
        		Statement stmt = conn.createStatement();
        		ResultSet rs = stmt.executeQuery("select * from BLOCKCHAIN");
	            while (rs.next()) {
	            	result += "ID: " + rs.getInt("ID") + " | Chain: " + rs.getString("CHAIN") + "\n";
	            }
	            System.out.println(result);
        	} catch (SQLException sqle) {
        		System.out.println(sqle.getMessage());
        	} catch (Exception e) {
        		System.out.println(e.getMessage());
        	} finally {
        		try {
    				if(conn != null) {
    					conn.close();
    					System.out.println("Connection closed!");
    				}
    			} catch (SQLException sqle) {
    				System.out.println(sqle.getMessage());
    			}
        	}
            res.status(200);
            return result;
        });
        //TODO: add certificates/signatures for making blockchain public
        //TODO: improve upon the merkle root implementation
    }

}

class TransactionHelper {
	@SerializedName("transaction")
	String data;
}
