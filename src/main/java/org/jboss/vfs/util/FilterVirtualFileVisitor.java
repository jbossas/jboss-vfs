/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VirtualFileFilterWithAttributes;
import org.jboss.vfs.VisitorAttributes;

/**
 * A visitor based on a virtual file filter
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class FilterVirtualFileVisitor extends AbstractVirtualFileVisitor {

    /**
     * The filter
     */
    private final VirtualFileFilter filter;

    /**
     * What is matched
     */
    private List<VirtualFile> matched;

    /**
     * Check the attributes
     *
     * @param filter the filter
     * @param attributes the attributes
     *
     * @return the attributes
     *
     * @throws IllegalArgumentException for a null filter
     */
    private static VisitorAttributes checkAttributes(VirtualFileFilter filter, VisitorAttributes attributes) {
        if (filter == null)
            throw new IllegalArgumentException("Null filter");
        // Specified
        if (attributes != null)
            return attributes;
        // From the filter
        if (filter instanceof VirtualFileFilterWithAttributes)
            return ((VirtualFileFilterWithAttributes) filter).getAttributes();
        // It will use the default
        return null;
    }

    /**
     * Create a new FilterVirtualFileVisitor with default attributes
     *
     * @param filter the filter
     *
     * @throws IllegalArgumentException if the filter is null
     */
    public FilterVirtualFileVisitor(VirtualFileFilter filter) {
        this(filter, null);
    }

    /**
     * Create a new FilterVirtualFileVisitor.
     *
     * @param filter the filter
     * @param attributes the attributes, uses the default if null
     *
     * @throws IllegalArgumentException if the filter is null
     */
    public FilterVirtualFileVisitor(VirtualFileFilter filter, VisitorAttributes attributes) {
        super(checkAttributes(filter, attributes));
        this.filter = filter;
    }

    /**
     * Get the matched files
     *
     * @return the matched files
     */
    public List<VirtualFile> getMatched() {
        if (matched == null)
            return Collections.emptyList();
        else
            return matched;
    }

    public void visit(VirtualFile virtualFile) {
        if (filter.accepts(virtualFile)) {
            if (matched == null)
                matched = new ArrayList<VirtualFile>();
            matched.add(virtualFile);
        }
    }
}
