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
 * An iterable enumeration wrapper.
 *
 * @param <T> the element type
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class EnumerationIterable<T> implements Iterable<T> {

    private final Enumeration<T> entries;

    EnumerationIterable(Enumeration<T> entries) {
        this.entries = entries;
    }

    public Iterator<T> iterator() {
        return new EnumerationIterator<T>(entries);
    }
}
