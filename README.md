# handytrowel

Scrape news articles, output extracted article text and normalized/processed
ngrams

## Requirements

-   Supports any environment that runs Java 7, i.e. Mac OS X, Linux, Windows,
    etc.
-   ~300MB of free space (the dependencies are large).

## Usage

```
# Clone the repository
git clone https://github.com/asimihsan/handytrowel.git
cd handytrowel

# Build the executable artifacts
./gradlew installApp

# Run it, point it at a news article URL
build/install/handytrowel/bin/handytrowel \
    'http://www.nytimes.com/2014/05/06/health/world-health-organization-polio-health-emergency.html'

# You should see JSON output to stdout of:
# - the extracted article body, 
# - links within the article body,
# - normalized/pre-processed tokens not on a very large stopword list.
```

## License

handytrowel is licensed under the Affero General Public License v3.0. Please
see the LICENSE file for more details.
