/** ========================================================================
  * handytrowel: src/test/java/network/TestHTMLFetcher.java
  * End-to-end tests for HTMLFetcher.
  * ========================================================================
  * Copyright (c) 2014, Asim Ihsan, All rights reserved.
  * <http://www.asimihsan.com>
  * https://github.com/asimihsan/handytrowel/blob/master/LICENSE
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  * ========================================================================
  */

package com.asimihsan.handytrowel.network;

import static org.junit.Assert.*;

import com.asimihsan.handytrowel.network.HTMLFetcher.HTMLFetcherBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


class ResourceReader {
    private ResourceReader() { }
    public static String getResourceAsString(String path) throws IOException {
        try (
                InputStream is = ResourceReader.class.getResourceAsStream("/network/empty_page.txt");
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII);
                Scanner scanner = new Scanner(isr);
            ) {
            scanner.useDelimiter("\\A");
            return scanner.next();
        }
    }
}


class HelloHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(
            ResourceReader.getResourceAsString("/network/empty_page.txt"));
    }
}


public class TestHTMLFetcher {
    private static Server server;
    private static URI serverUri;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // let connector pick an unused port #
        server.addConnector(connector);

        server.setHandler(new HelloHandler());

        // Start Server
        server.start();

        String host = connector.getHost();
        if (host == null) {
            host = "localhost";
        }
        int port = connector.getLocalPort();
        serverUri = new URI(String.format("http://%s:%d/",host,port));
    }

    @AfterClass
    public static void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Test
    public void testEmptyPage() {
        HTMLFetcher htmlFetcher = new HTMLFetcherBuilder()
        .timeoutMillis(30 * 10000)
        .build();
        String pageSource = null;
        try {
            pageSource = htmlFetcher.getPageSource(serverUri.toString());
        } catch (TimeoutException e) {
            fail("Did not expect a timeout exception!");
        }
        try {
            assertEquals(ResourceReader.getResourceAsString("/network/empty_page.txt"),
                         pageSource.replaceAll("\n", ""));
        } catch (IOException e) {
            fail("Getting test source unexpectedly threw an exception");
        }
    }
}