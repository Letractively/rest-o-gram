package rest.o.gram.commands;

import org.json.rpc.client.HttpJsonRpcClientTransport;
import rest.o.gram.tasks.CachePhotoTask;
import rest.o.gram.tasks.ITaskObserver;

/**
 * Created with IntelliJ IDEA.
 * User: Or
 * Date: 5/24/13
 */
public class CachePhotoCommand extends AsyncTaskRestogramCommand {

    public CachePhotoCommand(HttpJsonRpcClientTransport transport, ITaskObserver observer, String id, String originVenueId) {
        super(transport, observer);
        this.id = id;
        this.originVenueId = originVenueId;
    }

    @Override
    public boolean execute() {
        if(!super.execute())
            return false;

        CachePhotoTask t = new CachePhotoTask(transport, this);
        t.execute(id, originVenueId);

        task = t;

        return true;
    }

    private String id;
    private String originVenueId;
}
