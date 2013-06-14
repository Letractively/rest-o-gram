package rest.o.gram.commands;

import org.json.rpc.client.HttpJsonRpcClientTransport;
import rest.o.gram.tasks.AddPhotoToFavoritesTask;
import rest.o.gram.tasks.GetNearbyTask;
import rest.o.gram.tasks.ITaskObserver;

/**
 * Created with IntelliJ IDEA.
 * User: Itay
 * Date: 14/06/13
 */
public class AddPhotoToFavoritesCommand extends AsyncTaskRestogramCommand {

    public AddPhotoToFavoritesCommand(HttpJsonRpcClientTransport transport, ITaskObserver observer, String photoId) {
        super(transport, observer);
        this.photoId = photoId;
    }

    @Override
    public boolean execute() {
        if(!super.execute())
            return false;

        AddPhotoToFavoritesTask t = new AddPhotoToFavoritesTask(transport, this);

        t.execute(photoId);
        task = t;

        return true;
    }


    private String photoId;

}
