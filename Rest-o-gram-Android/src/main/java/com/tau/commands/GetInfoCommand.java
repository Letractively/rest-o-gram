package com.tau.commands;

import com.tau.tasks.GetInfoTask;
import com.tau.tasks.ITaskObserver;
import org.json.rpc.client.HttpJsonRpcClientTransport;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 15/04/13
 */
public class GetInfoCommand implements IRestogramCommand {

    public GetInfoCommand(String venueID) {
        this.venueID = venueID;
    }

    @Override
    public void execute(HttpJsonRpcClientTransport transport, ITaskObserver observer) {
        GetInfoTask task = new GetInfoTask(transport, observer);
        task.execute(venueID);
    }

    private String venueID;
}
