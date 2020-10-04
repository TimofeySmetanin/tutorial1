package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;


public class BaconPath implements HttpHandler{
	
	private Driver driver;
	private String uriDb;
	
	public BaconPath() {
		uriDb = "bolt://localhost:7687";
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","1234"));
	}
	
	public void handle(HttpExchange r) {
		try {
            if (r.getRequestMethod().equals("GET")) 
                handleGet(r);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void handleGet(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        String baconId = "nm0000102";
        String actorId;
        int baconNum;
        //no actorId or movieId inputed
        if (!deserialized.has("actorId")) {
        	r.sendResponseHeaders(400, -1);
        }
        else {
        	actorId = deserialized.getString("actorId");
        	
        	JSONObject response = new JSONObject();

        	try (Session session = driver.session()){
        		
        		Result result = session.run("MATCH (n.Actor) WHERE n.actorId = $actorId RETURN n.name", parameters("actorId", actorId));
        		
        		//checks if the actor exists in database
        		if (result.hasNext() == false) {
        			r.sendResponseHeaders(404, -1);
        		}
        		else {
        			//check if bacon id is bacon himself
        			if (actorId.equals(baconId)) {
        				baconNum = 0;
        				String baconNumber = String.valueOf(baconNum);
        				response.put("baconNumber", baconNumber);
        			}
        			//calculate baconNumber if it is 1 or greater
        			result = session.run("MATCH (k:Actor {actorId: $baconId}),(destination:Actor {actorId: $actorId}), p=shortestPath((k)-[:ACTED_IN*]-(actorId)"
        					+ "length(p)> 1 RETURN p as BaconPath, length([min nodes(p) WHERE m:Movie]) as BaconNumber", parameters("baconId", baconId, "actorId", actorId));
        			JSONArray BaconPath = new JSONArray();
        			
        			BaconPath.put(result.single().get("BaconPath").asString());
        			response.put("baconNumber", result.single().get("baconNumber").asString());
        			response.append("baconPath", BaconPath);
        		}
        	} catch (Exception e) {
        		r.sendResponseHeaders(500, -1);
        		
        	}
        	r.sendResponseHeaders(200, response.length());
        	OutputStream os = r.getResponseBody();
        	os.write(response.toString().getBytes("utf-8"));
        	os.close();
        	
        }
	}
}