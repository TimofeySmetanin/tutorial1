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


public class Movie implements HttpHandler{
	
	private Driver driver;
	private String uriDb;
	
	public Movie() {
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
        String actor;
        String movieId;
        
        //no movieId inputed
        if (!deserialized.has("movieId")) {
        	r.sendResponseHeaders(400, -1);
        }
        else {
        	movieId = deserialized.getString("movieId");
        	
        	JSONObject response = new JSONObject();
        	
        	
        	
        	try (Session session = driver.session()){
        		
        		Result result = session.run("MATCH (n.Movie) WHERE n.movieId = $movieId RETURN n.name", parameters("movieId", movieId));
        		if (result.hasNext() == false) {
        			r.sendResponseHeaders(404, -1);
        		}
        		else {
        			//add name and id to JSON response
        			name = result.toString();
        			response.put("movieId", movieId);
        			response.append("name", name);
        			//search for all actors that played a role in the movie
        			result = session.run("MATCH (:Movie {movieId: $movieId})-[ACT]->(n.Actor) RETURN n.actorId as actorId", parameters("movieId", movieId));
        			JSONArray actorsInMovie = new JSONArray();
        			while (result.hasNext() == true) {
        				actor = result.next().get("actorID").asString();
        				actorsInMovie.put(actor);
        				
        			}
        			response.append("actors", actorsInMovie);
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
		if (!deserialized.has("name") || !deserialized.has("movieId")) {
			r.sendResponseHeaders(400, -1);
		}
		String name = deserialized.getString("name");
		String movieId = deserialized.getString("movieId");
		
		
		try (Session session = driver.session()){
			//check if actor is already in database
			Result result = session.run("MATCH (n.Movie) WHERE n.movieId = $actorId RETURN n.name", parameters("movieId", movieId));
    		if (result.hasNext() == true) {
    			r.sendResponseHeaders(404, -1);
    		}
    		//add actor to database
    		session.writeTransaction(tx -> tx.run("MERGE (a:Movie {name: $name, movieId: $movieId})", parameters("name", name, "movieId", movieId)));
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
