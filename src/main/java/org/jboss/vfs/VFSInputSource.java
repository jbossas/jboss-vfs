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
package org.jboss.vfs;

import java.io.InputStream;

import org.jboss.vfs.util.LazyInputStream;
import org.xml.sax.InputSource;

/**
 * VFS based impl of InputSource.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class VFSInputSource extends InputSource {

    private VirtualFile file;

    public VFSInputSource(VirtualFile file) {
        if (file == null) {
            throw VFSMessages.MESSAGES.nullArgument("file");
        }
        this.file = file;
    }

    @Override
    public String getSystemId() {
        try {
            return VFSUtils.getVirtualURI(file).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getByteStream() {
        return new LazyInputStream(file);
    }

    @Override
    public String toString() {
        return file.getPathName();
    }
}
