/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.vfs.spi;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * An enumeration iterator.
 *
 * @param <T> the element type
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class EnumerationIterator<T> implements Iterator<T> {

    private final Enumeration<T> entries;

    EnumerationIterator(Enumeration<T> entries) {
        this.entries = entries;
    }

    public boolean hasNext() {
        return entries.hasMoreElements();
    }

    public T next() {
        return entries.nextElement();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
