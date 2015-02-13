package org.lable.util.uniqueid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

/**
 * A caching wrapper around an {@link org.lable.util.uniqueid.IDGenerator} instance.
 * <p/>
 * This class will cache a bunch of generated IDs and automatically refill the stack when it runs out. By letting
 * this class handle the caching, calling classes can simply call {@link #generate()} whenever a new ID is needed,
 * without having to worry about any performance hit you might see when calling
 * {@link IDGenerator#generate()} repeatedly from a time-consuming loop.
 */
public class AutoRefillStack implements IDGenerator {

    static final int DEFAULT_BATCH_SIZE = 500;

    final int batchSize;
    final IDGenerator generator;
    final Deque<byte[]> idStack = new ArrayDeque<byte[]>();

    protected AutoRefillStack(IDGenerator generator, int batchSize) {
        this.batchSize = batchSize;
        this.generator = generator;
    }

    /**
     * Wrap an {@link IDGenerator} in an AutoRefillStack, with a default batch-size.
     *
     * @param generator Generator to decorate.
     * @return The decorated generator.
     */
    public static IDGenerator decorate(IDGenerator generator) {
        return new AutoRefillStack(generator, DEFAULT_BATCH_SIZE);
    }

    /**
     * Wrap an {@link IDGenerator} in an AutoRefillStack, with a specific batch size.
     *
     * @param generator Generator to decorate.
     * @param batchSize The amount of IDs to cache.
     * @return The decorated generator.
     */
    public static IDGenerator decorate(IDGenerator generator, int batchSize) {
        return new AutoRefillStack(generator, batchSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized byte[] generate() throws GeneratorException {
        return popOne();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Deque<byte[]> batch(int size) throws GeneratorException {
        if (size < 0) {
            size = 0;
        }
        Deque<byte[]> batch = new ArrayDeque<byte[]>(size);
        while (size > 0) {
            batch.add(popOne());
            size--;
        }
        return batch;
    }

    /**
     * Grab a single ID from the stack. If the stack is empty, load up a new batch from the wrapped generator.
     *
     * @return A single ID.
     * @throws GeneratorException
     */
    byte[] popOne() throws GeneratorException {
        try {
            return idStack.pop();
        } catch (NoSuchElementException e) {
            // Cached stack is empty, load up a fresh stack.
            idStack.addAll(generator.batch(batchSize));
            return popOne();
        }
    }
}
