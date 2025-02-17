package info.cotr.gdx;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class GameThread extends Thread {
    private final ReentrantLock lock;
    private final Condition condition;  // Each condition is a waiting queue
    private final String name;
    // volatile means always read and written from main memory to ensure consistent thread to variable access.
    private volatile boolean gameOn;

    public GameThread(ReentrantLock lock, Condition condition, String name) {
        this.lock = lock;
        this.condition = condition;
        this.name = name;
        this.gameOn = true;
    }

    @Override
    public void run() {
        while (this.gameOn) {
            lock.lock();  // Acquire lock to make sure this is a serial action.
            try {
                System.out.println(name + ": Waiting for condition");
                condition.await();  // Wait to be notified that this condition is satisfied.
                if(!this.gameOn) {  // Check to make sure the game isn't over before doing any processing.
                    break;  // If this is a signal indicating the game is over, exit the loop.
                }
                System.out.println(name + ": doing something");
                Thread.sleep(1000); // Do something.
            } catch (InterruptedException ignored) { // Don't care about interrupts at this point.
            } finally {
                lock.unlock();
            }
        }
        System.out.println(name + ": done doing stuff.");
    }

    public void gameOver() {
        this.gameOn = false;
    }
}
