/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.jboss.vfs.VFSLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;

/**
 * Include/exclude visitor attributes.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class IncludeExcludeVisitorAttributes extends VisitorAttributes implements VirtualFileFilter {

    private Set<String> includes;
    private Set<String> excludes;

    public IncludeExcludeVisitorAttributes(Set<String> includes, Set<String> excludes) {
        if (includes == null) { includes = Collections.emptySet(); }
        if (excludes == null) { excludes = Collections.emptySet(); }

        this.includes = includes;
        this.excludes = excludes;

        setIncludeRoot(false);
        setLeavesOnly(true);
        setRecurseFilter(this);
    }

    public boolean accepts(VirtualFile file) {
        try {
            URL url = file.toURL();
            String urlString = url.toExternalForm();

            for (String include : includes) {
                if (urlString.contains(include) == false) { return false; }
            }

            for (String exclude : excludes) {
                if (urlString.contains(exclude)) { return false; }
            }

            return true;
        } catch (Exception e) {
            VFSLogger.ROOT_LOGGER.tracef(e,"Exception while filtering file: %s", file);
            return false;
        }
    }
}