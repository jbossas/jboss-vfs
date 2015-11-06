/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.jboss.vfs.VFSLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;

/**
 * Matches a file name against a list of suffixes.
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 44223 $
 */
public class SuffixMatchFilter extends AbstractVirtualFileFilterWithAttributes {

    /**
     * The suffixes
     */
    private Collection<String> suffixes;
    private boolean trace;

    /**
     * Create a new SuffixMatchFilter, using {@link VisitorAttributes#DEFAULT}
     *
     * @param suffix the suffix
     * @throws IllegalArgumentException for a null suffix
     */
    public SuffixMatchFilter(String suffix) {
        this(suffix, null);
    }

    /**
     * Create a new SuffixMatchFilter.
     *
     * @param suffix     the suffix
     * @param attributes the attributes, pass null to use {@link VisitorAttributes#DEFAULT}
     * @throws IllegalArgumentException for a null suffix
     */
    @SuppressWarnings("unchecked")
    public SuffixMatchFilter(String suffix, VisitorAttributes attributes) {
        this(Collections.singleton(suffix), attributes);
    }

    /**
     * Create a new SuffixMatchFilter.
     *
     * @param suffixes - the list of file suffixes to accept.
     * @throws IllegalArgumentException for a null suffixes
     */
    public SuffixMatchFilter(Collection<String> suffixes) {
        this(suffixes, null);
    }

    /**
     * Create a new SuffixMatchFilter.
     *
     * @param suffixes   - the list of file suffixes to accept.
     * @param attributes the attributes, pass null to use {@link VisitorAttributes#DEFAULT}
     * @throws IllegalArgumentException for a null suffixes
     */
    public SuffixMatchFilter(Collection<String> suffixes, VisitorAttributes attributes) {
        super(attributes == null ? VisitorAttributes.DEFAULT : attributes);
        if (suffixes == null) {
            throw MESSAGES.nullArgument("suffixes");
        }
        this.suffixes = new LinkedHashSet<String>();
        this.suffixes.addAll(suffixes);
    }

    /**
     * Accept any file that ends with one of the filter suffixes. This checks that the file.getName() endsWith a suffix.
     *
     * @return true if the file matches a suffix, false otherwise.
     */
    public boolean accepts(VirtualFile file) {
        String name = file.getName();
        boolean accepts = false;
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                accepts = true;
                break;
            }
        }
            VFSLogger.ROOT_LOGGER.tracef("%s accepted: %s", file, accepts);
        return accepts;
    }
}
