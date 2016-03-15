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
package org.jboss.vfs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.jboss.vfs.spi.FileSystem;
import org.jboss.vfs.spi.MountHandle;

/**
 * MountHandle implementation.  Provides the default behavior
 * of delegating to the FileSystem to get the mount source as
 * well as cleaning up resources.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 */
class BasicMountHandle implements MountHandle {
    private final FileSystem fileSystem;
    private final Closeable mountHandle;
    private final Closeable[] closeables;

    /**
     * Create new DefaultMountHandle with a FileSystem and an array of closeable.
     *
     * @param fileSystem           to use to retrieve the mount source
     * @param mountHandle          the handle to close the actual mount
     * @param additionalCloseables addition Closeable to execute on close
     */
    BasicMountHandle(final FileSystem fileSystem, Closeable mountHandle, Closeable... additionalCloseables) {
        this.fileSystem = fileSystem;
        this.mountHandle = mountHandle;
        this.closeables = additionalCloseables;
    }

    /* {@inheritDoc} */
    public File getMountSource() {
        return fileSystem.getMountSource();
    }

    /* {@inheritDoc} */
    public void close() throws IOException {
        VFSUtils.safeClose(fileSystem);
        VFSUtils.safeClose(mountHandle);
        for (Closeable closeable : closeables) {
            VFSUtils.safeClose(closeable);
        }
    }
}
