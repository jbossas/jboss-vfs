/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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

import static org.jboss.vfs.VFSMessages.MESSAGES;

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
     * @param filter     the filter
     * @param attributes the attributes
     * @return the attributes
     * @throws IllegalArgumentException for a null filter
     */
    private static VisitorAttributes checkAttributes(VirtualFileFilter filter, VisitorAttributes attributes) {
        if (filter == null) {
            throw MESSAGES.nullArgument("filter");
        }
        // Specified
        if (attributes != null) { return attributes; }
        // From the filter
        if (filter instanceof VirtualFileFilterWithAttributes) { return ((VirtualFileFilterWithAttributes) filter).getAttributes(); }
        // It will use the default
        return null;
    }

    /**
     * Create a new FilterVirtualFileVisitor with default attributes
     *
     * @param filter the filter
     * @throws IllegalArgumentException if the filter is null
     */
    public FilterVirtualFileVisitor(VirtualFileFilter filter) {
        this(filter, null);
    }

    /**
     * Create a new FilterVirtualFileVisitor.
     *
     * @param filter     the filter
     * @param attributes the attributes, uses the default if null
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
        if (matched == null) { return Collections.emptyList(); } else { return matched; }
    }

    public void visit(VirtualFile virtualFile) {
        if (filter.accepts(virtualFile)) {
            if (matched == null) { matched = new ArrayList<VirtualFile>(); }
            matched.add(virtualFile);
        }
    }
}
