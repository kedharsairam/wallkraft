//! Wallhaven API client (port of the Android app's WallhavenApi).

use std::time::Duration;

use anyhow::Result;
use reqwest::Client;

use crate::model::{SearchResponse, Wallpaper};

const BASE_URL: &str = "https://wallhaven.cc/api/v1";

/// Connect timeout for a single request.
const CONNECT_TIMEOUT: Duration = Duration::from_secs(15);
/// Total timeout for a single request (connect + body).
const REQUEST_TIMEOUT: Duration = Duration::from_secs(30);
/// How many times a request is retried before giving up.
const MAX_RETRIES: u32 = 3;
/// Base delay for exponential backoff: 500ms, 1s, 2s.
const BACKOFF_BASE_MS: u64 = 500;

/// Browse/search filters, mirroring the Android app's WallhavenFilters.
#[derive(Clone, Debug)]
pub struct Filters {
    /// Categories bitmask as "abc": 1 = general, 2 = anime, 4 = people.
    pub categories: String,
    /// Purity bitmask as "abc": 1 = sfw, 2 = sketchy, 4 = nsfw.
    pub purity: String,
    /// date_added | relevance | views | random | favorites.
    pub sorting: String,
    /// "" (all) | "landscape" | "portrait".
    pub orientation: String,
    /// Dominant color as hex without '#', e.g. "771c1c". Empty = any.
    pub color: String,
    /// Minimum resolution, e.g. "1920x1080". Empty = any.
    pub atleast: String,
    /// Aspect ratio as "WxH", e.g. "16x9". Empty = any.
    pub ratios: String,
}

impl Default for Filters {
    fn default() -> Self {
        Self {
            categories: "111".into(),
            purity: "100".into(),
            sorting: "date_added".into(),
            orientation: String::new(),
            color: String::new(),
            atleast: String::new(),
            ratios: String::new(),
        }
    }
}

#[derive(Clone)]
pub struct WallhavenClient {
    client: Client,
    api_key: Option<String>,
}

impl WallhavenClient {
    pub fn new(api_key: Option<String>) -> Result<Self> {
        let client = Client::builder()
            .user_agent("WallKraft/0.1 (Windows)")
            .connect_timeout(CONNECT_TIMEOUT)
            .timeout(REQUEST_TIMEOUT)
            .build()?;
        Ok(Self { client, api_key })
    }

    /// Search the Wallhaven library. `page` is 1-based.
    pub async fn search(&self, query: &str, page: u32, filters: &Filters) -> Result<Vec<Wallpaper>> {
        let mut url = reqwest::Url::parse(&format!("{BASE_URL}/search"))?;
        {
            let mut pairs = url.query_pairs_mut();
            pairs
                .append_pair("q", query)
                .append_pair("page", &page.to_string())
                .append_pair("categories", &filters.categories)
                .append_pair("purity", &filters.purity)
                .append_pair("sorting", &filters.sorting)
                .append_pair("order", "desc");
            if !filters.orientation.is_empty() {
                pairs.append_pair("aspectRatio", &filters.orientation);
            }
            if !filters.color.is_empty() {
                pairs.append_pair("colors", &filters.color);
            }
            if !filters.atleast.is_empty() {
                pairs.append_pair("atleast", &filters.atleast);
            }
            if !filters.ratios.is_empty() {
                pairs.append_pair("ratios", &filters.ratios);
            }
            if let Some(key) = &self.api_key {
                pairs.append_pair("apikey", key);
            }
        }
        let body: SearchResponse = self
            .retry(|| async {
                let resp = self.client.get(url.clone()).send().await?;
                let body: SearchResponse = resp.error_for_status()?.json().await?;
                Ok::<_, anyhow::Error>(body)
            })
            .await?;
        Ok(body.data)
    }

    /// Download a URL to a local file. Returns the destination path.
    pub async fn download_to(&self, url: &str, dest: &std::path::Path) -> Result<()> {
        let dest = dest.to_path_buf();
        let bytes = self
            .retry(|| async {
                let resp = self.client.get(url).send().await?;
                let bytes = resp.error_for_status()?.bytes().await?;
                Ok::<_, anyhow::Error>(bytes)
            })
            .await?;
        std::fs::write(&dest, &bytes)?;
        Ok(())
    }

    /// Run `op` with exponential backoff on transient failures (network
    /// errors, timeouts, 5xx, 429). Non-transient errors (4xx other than 429)
    /// fail immediately.
    async fn retry<T, F, Fut>(&self, op: F) -> Result<T>
    where
        F: Fn() -> Fut,
        Fut: std::future::Future<Output = Result<T>>,
    {
        let mut attempt = 0;
        loop {
            match op().await {
                Ok(v) => return Ok(v),
                Err(e) if attempt < MAX_RETRIES && is_retryable(&e) => {
                    attempt += 1;
                    let delay = BACKOFF_BASE_MS * 2u64.pow(attempt - 1);
                    tracing::warn!("request failed (attempt {attempt}/{}): {e:#}; retrying in {delay}ms", MAX_RETRIES + 1);
                    tokio::time::sleep(Duration::from_millis(delay)).await;
                }
                Err(e) => return Err(e),
            }
        }
    }
}

/// Whether an error is worth retrying: transport/timeout errors and 5xx/429
/// responses. 4xx (bad request, not found, etc.) are permanent.
fn is_retryable(e: &anyhow::Error) -> bool {
    if let Some(re) = e.downcast_ref::<reqwest::Error>() {
        if let Some(status) = re.status() {
            return status.is_server_error() || status.as_u16() == 429;
        }
        return re.is_timeout() || re.is_connect() || re.is_request();
    }
    // Unknown error type — assume transient rather than fail fast.
    true
}

#[cfg(test)]
mod tests {
    use super::is_retryable;
    use anyhow::anyhow;

    #[tokio::test]
    async fn retryable_errors() {
        // Connection refused on a closed port is a transport error → retryable.
        let timeout = reqwest::Client::builder()
            .build()
            .unwrap()
            .get("http://127.0.0.1:1")
            .send()
            .await
            .unwrap_err();
        assert!(is_retryable(&anyhow!(timeout)), "connect error should be retryable");
    }

    #[test]
    fn non_retryable_errors() {
        // A plain error with no status is treated as retryable (conservative),
        // but a 4xx status is permanent.
        let e = anyhow!("not found");
        assert!(is_retryable(&e));
    }
}