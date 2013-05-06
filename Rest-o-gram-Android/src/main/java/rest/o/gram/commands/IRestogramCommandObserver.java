package rest.o.gram.commands;

/**
 * Created with IntelliJ IDEA.
 * User: Roi
 * Date: 06/05/13
 */
public interface IRestogramCommandObserver {

    /**
     * Called after command has finished executing
     */
    void onFinished(IRestogramCommand command);

    /**
     * Called after command has encountered an error
     */
    void onError(IRestogramCommand command);
}
