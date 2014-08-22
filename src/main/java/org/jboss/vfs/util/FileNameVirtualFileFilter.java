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

import static org.jboss.vfs.VFSMessages.MESSAGES;

import java.util.Map;
import java.util.Set;

import org.jboss.vfs.VFSLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Exclude virtual file by file name and path.
 *
 * @author ales.justin@jboss.org
 */
public class FileNameVirtualFileFilter implements VirtualFileFilter {
    private Map<String, Set<String>> excludes;

    public FileNameVirtualFileFilter(Map<String, Set<String>> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            throw MESSAGES.nullOrEmpty("excludes");
        }

        this.excludes = excludes;
    }

    /**
     * Do we accept file.
     * <p/>
     * If pathName contains any of the keys,
     * * if the value is null - then do exclude
     * * if value is not null - only exclude if it value contains simple name
     *
     * @param file the virtual file
     * @return false if file is excluded by excludes map, true other wise
     */
    public boolean accepts(VirtualFile file) {
        String pathName = getPathName(file);
        for (Map.Entry<String, Set<String>> entry : excludes.entrySet()) {
            String key = entry.getKey();
            if (pathName.contains(key)) {
                String simpleName = file.getName();
                Set<String> value = entry.getValue();
                if (value == null || value.contains(simpleName)) {
                    VFSLogger.ROOT_LOGGER.tracef("Excluding %s", pathName);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get the path name for the VirtualFile.
     *
     * @param file the virtual file
     * @return the path name
     */
    protected String getPathName(VirtualFile file) {
        try {
            // prefer the URI, as the pathName might
            // return an empty string for temp virtual files
            return file.toURI().toString();
        } catch (Exception e) {
            return file.getPathName();
        }
    }
}