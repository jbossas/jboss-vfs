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
package org.jboss.vfs.protocol;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Implements basic URLConnection for a VirtualFile
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
class VirtualFileURLConnection extends AbstractURLConnection {
    private final VirtualFile file;

    VirtualFileURLConnection(URL url) throws IOException {
        super(url);
        file = VFS.getChild(toURI(url));
    }

    public void connect() throws IOException {
    }

    public Object getContent() throws IOException {
        if (getContentType() != null) {
            return super.getContent();
        }
        return file;
    }

    public int getContentLength() {
        final long size = file.getSize();
        return size > (long) Integer.MAX_VALUE ? -1 : (int) size;
    }

    public long getLastModified() {
        return file.getLastModified();
    }

    public InputStream getInputStream() throws IOException {
        return file.openStream();
    }

    public Permission getPermission() throws IOException {
        String decodedPath = toURI(url).getPath();
        if (File.separatorChar != '/') { decodedPath = decodedPath.replace('/', File.separatorChar); }

        return new FilePermission(decodedPath, "read");
    }

    @Override
    protected String getName() {
        return file.getName();
    }
}
