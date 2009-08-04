/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.vfs.protocol;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.net.Proxy;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

/**
 * The VFS URL stream handler.
 */
public abstract class VirtualFileURLStreamHandler extends URLStreamHandler {

    private static final Set<String> locals;

    static {
        Set<String> set = new HashSet<String>();
        set.add(null);
        set.add("");
        set.add("~");
        set.add("localhost");
        locals = set;
    }

    protected URLConnection openConnection(URL u) throws IOException {
        if (locals.contains(toLower(u.getHost()))) {
            // the URL is a valid local URL
            return new VirtualFileURLConnection(u);
        }
        throw new IOException("Remote host access not supported for URLs of type \"" + u.getProtocol() + "\"");
    }

    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        return openConnection(u);
    }

    @Override
    protected boolean hostsEqual(URL url1, URL url2) {
        return locals.contains(toLower(url1.getHost())) && locals.contains(toLower(url2.getHost())) || super.hostsEqual(url1, url2);
    }

    private static String toLower(String str) {
        return str == null ? null : str.toLowerCase();
    }
}
