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

import static org.jboss.vfs.VFSMessages.MESSAGES;

import java.util.Collection;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Filters out a set of suffixes
 *
 * @author adrian@jboss.org
 * @version $Revision: 44223 $
 */
public class SuffixesExcludeFilter implements VirtualFileFilter {

    /**
     * The suffixes
     */
    private Collection<String> suffixes;

    /**
     * Create a new SuffixMatchFilter,
     *
     * @param suffixes the suffixes
     * @throws IllegalArgumentException for null suffixes
     */
    public SuffixesExcludeFilter(Collection<String> suffixes) {
        if (suffixes == null) {
            throw MESSAGES.nullArgument("suffixes");
        }
        for (String suffix : suffixes) {
            if (suffix == null) {
                throw new IllegalArgumentException("Null suffix in " + suffixes);
            }
        }
        this.suffixes = suffixes;
    }

    public boolean accepts(VirtualFile file) {
        String name = file.getName();
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) { return false; }
        }
        return true;
    }
}
