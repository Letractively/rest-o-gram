package rest.o.gram.commands;

import org.json.rpc.client.HttpJsonRpcClientTransport;
import rest.o.gram.tasks.ITaskObserver;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 06/05/13
 */
public abstract class AbstractRestogramCommand implements IRestogramCommand {

    public AbstractRestogramCommand() {
        observers = new HashSet<>();
        state = State.CS_Pending;
    }

    public AbstractRestogramCommand(HttpJsonRpcClientTransport transport, ITaskObserver observer) {
        this();
        this.transport = transport;
        this.observer = observer;
    }

    @Override
    public void execute() {
        state = State.CS_Executing;
    }

    @Override
    public void addObserver(IRestogramCommandObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(IRestogramCommandObserver observer) {
        observers.remove(observer);
    }

    @Override
    public State state() {
        return state;
    }

    /**
     * Notifies observers on finished event
     */
    protected void notifyFinished() {
        state = State.CS_Finished;
        for(IRestogramCommandObserver o : observers)
            o.onFinished(this);
    }

    /**
     * Notifies observers on error event
     */
    protected void notifyError() {
        state = State.CS_Failed;
        for(IRestogramCommandObserver o : observers)
            o.onError(this);
    }

    protected HttpJsonRpcClientTransport transport; // Transport object
    protected ITaskObserver observer; // Task observer object

    private State state; // Task state
    private Set<IRestogramCommandObserver> observers; // Command observers
}
