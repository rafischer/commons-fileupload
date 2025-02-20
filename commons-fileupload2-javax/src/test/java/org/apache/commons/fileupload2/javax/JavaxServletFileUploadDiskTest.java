/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.fileupload2.javax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload2.core.AbstractFileUploadTest;
import org.apache.commons.fileupload2.core.Constants;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.disk.DiskFileItem;
import org.apache.commons.fileupload2.core.disk.DiskFileItemFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JavaxServletFileUpload}.
 *
 * @see AbstractFileUploadTest
 */
public class JavaxServletFileUploadDiskTest extends AbstractFileUploadTest<JavaxServletDiskFileUpload, HttpServletRequest, DiskFileItem, DiskFileItemFactory> {

    public JavaxServletFileUploadDiskTest() {
        super(new JavaxServletDiskFileUpload());
    }

    @Test
    public void parseImpliedUtf8() throws Exception {
        // utf8 encoded form-data without explicit content-type encoding
        // @formatter:off
        final String text = "-----1234\r\n" +
                "Content-Disposition: form-data; name=\"utf8Html\"\r\n" +
                "\r\n" +
                "Thís ís the coñteñt of the fíle\n" +
                "\r\n" +
                "-----1234--\r\n";
        // @formatter:on

        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        final HttpServletRequest request = new JavaxMockHttpServletRequest(bytes, Constants.CONTENT_TYPE);
        // @formatter:off
        final DiskFileItemFactory fileItemFactory = DiskFileItemFactory.builder()
                .setCharset(StandardCharsets.UTF_8)
                .get();
        // @formatter:on
        final JavaxServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new JavaxServletFileUpload<>(fileItemFactory);
        final List<DiskFileItem> fileItems = upload.parseRequest(request);
        final DiskFileItem fileItem = fileItems.get(0);
        assertTrue(fileItem.getString().contains("coñteñt"), fileItem.getString());
    }

    /*
     * Test case for <a href="https://issues.apache.org/jira/browse/FILEUPLOAD-210">
     */
    @Test
    public void parseParameterMap() throws Exception {
        // @formatter:off
        final String text = "-----1234\r\n" +
                      "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
                      "Content-Type: text/whatever\r\n" +
                      "\r\n" +
                      "This is the content of the file\n" +
                      "\r\n" +
                      "-----1234\r\n" +
                      "Content-Disposition: form-data; name=\"field\"\r\n" +
                      "\r\n" +
                      "fieldValue\r\n" +
                      "-----1234\r\n" +
                      "Content-Disposition: form-data; name=\"multi\"\r\n" +
                      "\r\n" +
                      "value1\r\n" +
                      "-----1234\r\n" +
                      "Content-Disposition: form-data; name=\"multi\"\r\n" +
                      "\r\n" +
                      "value2\r\n" +
                      "-----1234--\r\n";
        // @formatter:on
        final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        final HttpServletRequest request = new JavaxMockHttpServletRequest(bytes, Constants.CONTENT_TYPE);

        final JavaxServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new JavaxServletFileUpload<>(DiskFileItemFactory.builder().get());
        final Map<String, List<DiskFileItem>> mappedParameters = upload.parseParameterMap(request);
        assertTrue(mappedParameters.containsKey("file"));
        assertEquals(1, mappedParameters.get("file").size());

        assertTrue(mappedParameters.containsKey("field"));
        assertEquals(1, mappedParameters.get("field").size());

        assertTrue(mappedParameters.containsKey("multi"));
        assertEquals(2, mappedParameters.get("multi").size());
    }

    @Override
    public List<DiskFileItem> parseUpload(final JavaxServletDiskFileUpload upload, final byte[] bytes, final String contentType) throws FileUploadException {
        final HttpServletRequest request = new JavaxMockHttpServletRequest(bytes, contentType);
        return upload.parseRequest(new JavaxServletRequestContext(request));
    }

    /**
     * Runs a test with varying file sizes.
     */
    @Override
    @Test
    public void testFileUpload() throws IOException, FileUploadException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int add = 16;
        int num = 0;
        for (int i = 0; i < 16384; i += add) {
            if (++add == 32) {
                add = 16;
            }
            final String header = "-----1234\r\n" + "Content-Disposition: form-data; name=\"field" + (num++) + "\"\r\n" + "\r\n";
            baos.write(header.getBytes(StandardCharsets.US_ASCII));
            for (int j = 0; j < i; j++) {
                baos.write((byte) j);
            }
            baos.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        baos.write("-----1234--\r\n".getBytes(StandardCharsets.US_ASCII));

        final List<DiskFileItem> fileItems = parseUpload(new JavaxServletDiskFileUpload(), baos.toByteArray());
        final Iterator<DiskFileItem> fileIter = fileItems.iterator();
        add = 16;
        num = 0;
        for (int i = 0; i < 16384; i += add) {
            if (++add == 32) {
                add = 16;
            }
            final DiskFileItem item = fileIter.next();
            assertEquals("field" + (num++), item.getFieldName());
            final byte[] bytes = item.get();
            assertEquals(i, bytes.length);
            for (int j = 0; j < i; j++) {
                assertEquals((byte) j, bytes[j]);
            }
        }
        assertTrue(!fileIter.hasNext());
    }

}
