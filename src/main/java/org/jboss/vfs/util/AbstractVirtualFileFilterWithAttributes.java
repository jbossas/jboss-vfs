/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs.util;

import org.jboss.vfs.VirtualFileFilterWithAttributes;
import org.jboss.vfs.VisitorAttributes;

/**
 * AbstractVirtualFileFilterWithAttributes
 *
 * @author adrian@jboss.org
 * @version $Revision: 44223 $
 */
public abstract class AbstractVirtualFileFilterWithAttributes implements VirtualFileFilterWithAttributes {

    /**
     * The attributes
     */
    private VisitorAttributes attributes;

    /**
     * Create a new AbstractVirtualFileFilterWithAttributes, using {@link VisitorAttributes#DEFAULT}
     */
    protected AbstractVirtualFileFilterWithAttributes() {
        this(null);
    }

    /**
     * Create a new AbstractVirtualFileFilterWithAttributes.
     *
     * @param attributes the attributes, pass null to use {@link VisitorAttributes#DEFAULT}
     */
    protected AbstractVirtualFileFilterWithAttributes(VisitorAttributes attributes) {
        if (attributes == null) { attributes = VisitorAttributes.DEFAULT; }
        this.attributes = attributes;
    }

    public VisitorAttributes getAttributes() {
        return attributes;
    }
}
