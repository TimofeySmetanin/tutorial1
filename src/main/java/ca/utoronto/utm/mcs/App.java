package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        Actor a = new Actor();
        Movie m = new Movie();
        Relationship r = new Relationship();
        Bacon baconNumber = new Bacon();
        BaconPath baconPath = new BaconPath();
        
        server.createContext("/api/v1/addActor", a);
        server.createContext("/api/v1/addMovie", m);
        server.createContext("/api/v1/addRelationship", r);
        server.createContext("/api/v1/getActor", a);
        server.createContext("/api/v1/getMovie", m);
        server.createContext("/api/v1/hasRelationship", r);
        server.createContext("/api/v1/computeBaconNumber", baconNumber);
        server.createContext("/api/v1/computeBaconPath", baconPath);
        
        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
