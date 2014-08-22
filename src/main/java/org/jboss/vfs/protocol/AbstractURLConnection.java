/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.jboss.vfs.VFSUtils;

/**
 * Abstract base class for VFS URLConection impls.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 * @version $Revision$
 */
public abstract class AbstractURLConnection extends URLConnection {

    private String contentType;

    protected AbstractURLConnection(final URL url) {
        super(url);
    }

    public String getHeaderField(String name) {
        String headerField = null;
        if (name.equals("content-type")) {
            headerField = getContentType();
        } else if (name.equals("content-length")) {
            headerField = String.valueOf(getContentLength());
        } else if (name.equals("last-modified")) {
            long lastModified = getLastModified();
            if (lastModified != 0) {
                // return the last modified date formatted according to RFC 1123
                Date modifiedDate = new Date(lastModified);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                headerField = sdf.format(modifiedDate);
            }
        } else {
            headerField = super.getHeaderField(name);
        }
        return headerField;
    }

    public String getContentType() {
        if (contentType != null) { return contentType; }
        contentType = getFileNameMap().getContentTypeFor(getName());
        if (contentType == null) {
            try {
                InputStream is = getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                contentType = java.net.URLConnection.guessContentTypeFromStream(bis);
                bis.close();
            } catch (IOException e) { /* ignore */ }
        }
        return contentType;
    }

    protected static URI toURI(URL url) throws IOException {
        try {
            return VFSUtils.toURI(url);
        } catch (URISyntaxException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    protected abstract String getName();
}
