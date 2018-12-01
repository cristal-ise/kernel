package org.cristalise.kernel.entity.proxy;

/**
 * Implement this interface to receive all ProxyMessages of all Items
 */
public interface ProxyMessageListener {

    /**
     * Notify the Listener about the new ProxyMessage
     * @param msg the message received
     */
    void notifyMessage(ProxyMessage msg);
}
