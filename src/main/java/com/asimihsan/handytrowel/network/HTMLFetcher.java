/** ========================================================================
  * handytrowel: src/main/java/cli/Main.java
  * Retrieve HTML source code of pages whilst executing JavaScript payload
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

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Retrieve the HTML source code of a web page after also executing its
 * JavaScript payload.
 *
 * @author Asim Ihsan
 */
public class HTMLFetcher {

    /**
     * How long to attempt to HTTP GET a page before timing out. This time
     * could be taken up both by HTTP latency and rendering and JavaScript
     * execution time.
     *
     * The default value is 30 seconds.
     */
    private final int timeoutMillis;

    public static class HTMLFetcherBuilder {
        private int timeoutMillis = 30 * 1000;

        public HTMLFetcherBuilder timeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public HTMLFetcher build() {
            return new HTMLFetcher(this);
        }
    }

    private HTMLFetcher(HTMLFetcherBuilder builder) {
        this.timeoutMillis = builder.timeoutMillis;
    }

    public String getPageSource(final String url) throws TimeoutException {

        // Make the Selenium WebDriver logs be quiet
        Logger logger = Logger.getLogger(PhantomJSDriverService.class.getName());
        logger.setLevel(Level.OFF);

        DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
        // What other CLI args there are: http://phantomjs.org/api/command-line.html
        // Where the cache goes on Mac OS X: ~/Library/Application\ Support/Ofi\ Labs/PhantomJS/
        // Other cache locations: https://groups.google.com/forum/#!topic/phantomjs/8GYaXKmowj0
        desiredCapabilities.setCapability(
            PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
            new String[] {"--ignore-ssl-errors=yes", "--load-images=no",
                          "--disk-cache=true", "--max-disk-cache-size=size=51200"
                         });
        final WebDriver driver = new PhantomJSDriver(desiredCapabilities);

        // doesn't work, keep as reference.
        //driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    driver.get(url);
                }
            });
            t.start();
            try {
                t.join(timeoutMillis);
            } catch (InterruptedException e) {
            }
            if (t.isAlive()) {
                System.out.println("Timeout for HTTP GET to: " + url);
                t.interrupt();
                throw new TimeoutException();
            }
            String pageSource = driver.getPageSource();
            return pageSource;
        } finally {
            driver.quit();
        }
    }

}
