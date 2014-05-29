package org.arcanum.util.concurrent;

import java.util.concurrent.Callable;

/**
 * @author Angelo De Caro (arcanumlib@gmail.com)
 * @since 1.0.0
 */
public interface Pool<T> {

    Pool<T> submit(Callable<T> callable);

    Pool<T> submit(Runnable runnable);

    Pool<T> awaitTermination();

}