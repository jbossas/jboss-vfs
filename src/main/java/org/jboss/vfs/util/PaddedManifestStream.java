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
package org.jboss.vfs.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Appends a new line char to the stream if it doesn't have one as his last byte.
 *
 * @author ehsavoie
 */
public class PaddedManifestStream extends InputStream {

    private final InputStream realStream;
    private int previousChar = -1;

    public PaddedManifestStream(InputStream realStream) {
        this.realStream = realStream;
    }

    @Override
    public int read() throws IOException {
        int value = this.realStream.read();
        if (value == -1 && previousChar != '\n' && previousChar != -1) {
            previousChar = '\n';
            return '\n';
        }
        previousChar = value;
        return value;
    }

    @Override
    public void close() throws IOException {
        super.close();
        realStream.close();
    }
}
