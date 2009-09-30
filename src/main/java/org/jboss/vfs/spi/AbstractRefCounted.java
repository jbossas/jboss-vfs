/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss, a division of Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.vfs.spi;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A resource which is reference-counted.
 */
public abstract class AbstractRefCounted implements Closeable {
    private volatile int refcount = 0;

    private static final AtomicIntegerFieldUpdater<AbstractRefCounted> refcountUpdater = AtomicIntegerFieldUpdater.newUpdater(AbstractRefCounted.class, "refcount");

    private static final int DEAD_VALUE = 0x87ffffff;
    private static final int MAX_REFCOUNT = 0x3fffffff;

    @SuppressWarnings({ "unchecked" })
    protected final <T extends AbstractRefCounted> Handle<T> doGetHandle() throws IOException {
        final int idx = refcountUpdater.getAndIncrement(this);
        if (idx < 0) {
            unreference();
            throw new IOException("Resource is already closed");
        }
        if (idx > MAX_REFCOUNT) {
            unreference();
            throw new IOException("Too many references to this resource");
        }
        return new RefHandle<T>((T) this);
    }

    void unreference() throws IOException {
        final int cnt = refcountUpdater.decrementAndGet(this);
        if (cnt == 0) {
            if (refcountUpdater.compareAndSet(this, 0, DEAD_VALUE)) {
                // we won the race to close, lucky us...
                doClose();
            }
        }
    }

    public final void close() throws IOException {
        if (refcountUpdater.getAndSet(this, DEAD_VALUE) >= 0) {
            doClose();
        }
    }

    public final boolean isOpen() {
        return refcount >= 0;
    }

    protected abstract void doClose() throws IOException;
}

final class RefHandle<T extends AbstractRefCounted> implements Handle<T> {
    private volatile T resource;

    private static final AtomicReferenceFieldUpdater<RefHandle, Object> resourceUpdater = AtomicReferenceFieldUpdater.newUpdater(RefHandle.class, Object.class, "resource");

    RefHandle(final T resource) {
        this.resource = resource;
    }

    public T getResource() throws IOException {
        final T resource = this.resource;
        if (resource == null) {
            throw new IOException("Handle is closed");
        }
        return resource;
    }

    public void close() throws IOException {
        final Object oldValue = resourceUpdater.getAndSet(this, null);
        if (oldValue != null) {
            ((AbstractRefCounted)oldValue).unreference();
        }
    }

    protected void finalize() throws Throwable {
        close();
    }
}
