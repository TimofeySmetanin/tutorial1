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


public class Relationship implements HttpHandler{
	
	private Driver driver;
	private String uriDb;
	
	public Relationship() {
		uriDb = "bolt://localhost:7687";
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","1234"));
	}
	
	public void handle(HttpExchange r) {
		try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        
        String movieId;
        String actorId;
        
        //no actorId or movieId inputed
        if (!deserialized.has("actorId") || !deserialized.has("movieId")) {
        	r.sendResponseHeaders(400, -1);
        }
        else {
        	actorId = deserialized.getString("actorId");
        	movieId = deserialized.getString("movieId");
        	JSONObject response = new JSONObject();
        	
        	
        	
        	try (Session session = driver.session()){
        		
        		Result result = session.run("MATCH (n.Actor) WHERE n.actorId = $actorId RETURN n.name", parameters("actorId", actorId));
        		Result result1 = session.run("MATCH (n.Movie) WHERE n.movieId = $movieId RETURN n.name", parameters("movieId", movieId));
        		//checks if the actor and movie exist in database
        		if (result.hasNext() == false || result1.hasNext() == false) {
        			r.sendResponseHeaders(404, -1);
        		}
        		else {
        			//add movieId and ActorId to JSON response
        			response.put("actorId", actorId);
        			response.append("movieId", movieId);
        			//search if actor acted in said movie
        			result = session.run("MATCH (:Actor {actorId: $actorId})-[r:ACTED_IN]->(:Movie {movieId: $movieId) RETURN b", parameters("actorId", actorId, "movieId", movieId));
        			
        			response.append("hasRelationship", result.hasNext());
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
	
	public void handlePut(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		
		//check if both variables exist
		if (!deserialized.has("movieId") || !deserialized.has("actorId")) {
			r.sendResponseHeaders(400, -1);
		}
		String movieId = deserialized.getString("movieId");
		String actorId = deserialized.getString("actorId");
		
		
		try (Session session = driver.session()){
			//check if actor is already in database
			Result result = session.run("MATCH (n.Actor) WHERE n.actorId = $actorId RETURN n.name", parameters("actorId", actorId));
			Result result1 = session.run("MATCH (n.Movie) WHERE n.movieId = $movieId RETURN n.name", parameters("movieId", movieId));
    		if (result.hasNext() == false || result1.hasNext() == false) {
    			r.sendResponseHeaders(404, -1);
    		}
    		//add actor ACTED_IN movie to database connection
    		session.writeTransaction(tx -> tx.run("MATCH (a:Actor {actorId: $actorId})," + "(m:Movie {movieId: $movieId})\n" + "MERGE (a)-[r:ACTED_IN]->(t)\n" + "RETURN r", parameters("actorId", actorId, "movieId", movieId)));
    		
    		session.close();
    		//500 Internal Server Error
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
			return;
		}
		r.sendResponseHeaders(200, -1);
	}

	public void close() {
		driver.close();
	}
}
