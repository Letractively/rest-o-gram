package rest.o.gram.client;

import rest.o.gram.commands.GetInfoCommand;
import rest.o.gram.commands.GetNearbyCommand;
import rest.o.gram.commands.GetPhotosCommand;
import rest.o.gram.commands.IRestogramCommand;
import rest.o.gram.location.ILocationTracker;
import rest.o.gram.location.LocationTrackerDummy;
import rest.o.gram.tasks.ITaskObserver;
import org.json.rpc.client.HttpJsonRpcClientTransport;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 15/04/13
 */
public class RestogramClient implements IRestogramClient {

    /**
     * Returns the singleton instance of restogram client
     */
    public static IRestogramClient getInstance() {
        if(instance == null)
            instance = new RestogramClient();

        return instance;
    }

    @Override
    public void getNearby(double latitude, double longitude, ITaskObserver observer) {
        IRestogramCommand command = new GetNearbyCommand(latitude, longitude);
        command.execute(transport, observer);
    }

    @Override
    public void getNearby(double latitude, double longitude, double radius, ITaskObserver observer) {
        IRestogramCommand command = new GetNearbyCommand(latitude, longitude, radius);
        command.execute(transport, observer);
    }

    @Override
    public void getInfo(String venueID, ITaskObserver observer) {
        IRestogramCommand command = new GetInfoCommand(venueID);
        command.execute(transport, observer);
    }

    @Override
    public void getPhotos(String venueID, ITaskObserver observer) {
        IRestogramCommand command = new GetPhotosCommand(venueID);
        command.execute(transport, observer);
    }

    @Override
    public ILocationTracker getLocationTracker() {
        return tracker;
    }

    /**
     * Ctor
     */
    private RestogramClient() {
        try {
            transport = new HttpJsonRpcClientTransport(new URL(url));
            tracker = new LocationTrackerDummy(); // TODO: create location tracker
        }
        catch(Exception e) {
            System.out.println("Error in RestogramClient: " + e.getMessage());
        }
    }

    private static IRestogramClient instance; // Singleton instance
    private final String url = "http://rest-o-gram.appspot.com/service"; // Server URL
    private HttpJsonRpcClientTransport transport; // Transport object
    private ILocationTracker tracker; // Location tracker
}