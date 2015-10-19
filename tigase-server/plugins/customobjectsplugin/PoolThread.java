package main.java.com.quickblox.chat.customobjectsplugin;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by QuickBlox team on 1/1/15.
 */
public class PoolThread extends Thread {

    private static final Logger log = Logger.getLogger(PoolThread.class.getName());

    private BlockingQueue<Runnable> taskQueue = null;
    private boolean isStopped = false;

    public PoolThread(BlockingQueue<Runnable> queue) {
        taskQueue = queue;
    }

    @Override
    public void run() {
        try {
            waitForTaskAndRunIt();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception " + e.toString());
        }
    }

    private void waitForTaskAndRunIt() throws InterruptedException {
        synchronized (this) {
            while (!isStopped()) {
                Runnable runnable = taskQueue.take();
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }

    @Override
    public void interrupt() {
        isStopped = true;
        super.interrupt();
    }

    public synchronized boolean isStopped() {
        return isStopped;
    }
}
