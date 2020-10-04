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


public class Actor implements HttpHandler{
	
	private Driver driver;
	private String uriDb;
	
	public Actor() {
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

        String name;
        String movie;
        String actorId;
        
        //no actorId inputed
        if (!deserialized.has("actorId")) {
        	r.sendResponseHeaders(400, -1);
        }
        else {
        	actorId = deserialized.getString("actorId");
        	
        	JSONObject response = new JSONObject();
        	
        	
        	
        	try (Session session = driver.session()){
        		
        		Result result = session.run("MATCH (n.Actor) WHERE n.actorId = $actorId RETURN n.name", parameters("actorId", actorId));
        		if (result.hasNext() == false) {
        			r.sendResponseHeaders(404, -1);
        		}
        		else {
        			//add name and id to JSON response
        			name = result.toString();
        			response.put("actorId", actorId);
        			response.append("name", name);
        			//search for all movie ids that actor has acted in
        			result = session.run("MATCH (:Actor {actorId: $actorId})-[ACTED_IN]->(n.Movie) RETURN n.movieId as movieId", parameters("actorId", actorId));
        			JSONArray moviesWithActor = new JSONArray();
        			while (result.hasNext() == true) {
        				movie = result.next().get("movieID").asString();
        				moviesWithActor.put(movie);
        				
        			}
        			response.append("movies", moviesWithActor);
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
		if (!deserialized.has("name") || !deserialized.has("actorId")) {
			r.sendResponseHeaders(400, -1);
		}
		String name = deserialized.getString("name");
		String actorId = deserialized.getString("actorId");
		
		
		try (Session session = driver.session()){
			//check if actor is already in database
			Result result = session.run("MATCH (n.Actor) WHERE n.actorId = $actorId RETURN n.name", parameters("actorId", actorId));
    		if (result.hasNext() == true) {
    			r.sendResponseHeaders(404, -1);
    		}
    		//add actor to database
    		session.writeTransaction(tx -> tx.run("MERGE (a:Actor {name: $name, actorId: $actorId})", parameters("name", name, "actorId", actorId)));
    		session.close();
    		//500 Internal Server Error
			
			
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
		}
		r.sendResponseHeaders(200, -1);
	}

	public void close() {
		driver.close();
	}
}
