//! Wallhaven API client (port of the Android app's WallhavenApi).

use anyhow::Result;
use reqwest::Client;

use crate::model::{SearchResponse, Wallpaper};

const BASE_URL: &str = "https://wallhaven.cc/api/v1";

#[derive(Clone)]
pub struct WallhavenClient {
    client: Client,
    api_key: Option<String>,
}

impl WallhavenClient {
    pub fn new(api_key: Option<String>) -> Result<Self> {
        let client = Client::builder()
            .user_agent("WallKraft/0.1 (Windows)")
            .build()?;
        Ok(Self { client, api_key })
    }

    /// Search the Wallhaven library. `page` is 1-based.
    pub async fn search(&self, query: &str, page: u32) -> Result<Vec<Wallpaper>> {
        let mut url = reqwest::Url::parse(&format!("{BASE_URL}/search"))?;
        {
            let mut pairs = url.query_pairs_mut();
            pairs
                .append_pair("q", query)
                .append_pair("page", &page.to_string())
                .append_pair("categories", "111")
                .append_pair("purity", "100")
                .append_pair("sorting", "relevance");
            if let Some(key) = &self.api_key {
                pairs.append_pair("apikey", key);
            }
        }
        let resp = self.client.get(url).send().await?;
        let body: SearchResponse = resp.error_for_status()?.json().await?;
        Ok(body.data)
    }

    /// Download a URL to a local file. Returns the destination path.
    pub async fn download_to(&self, url: &str, dest: &std::path::Path) -> Result<()> {
        let bytes = self.client.get(url).send().await?.error_for_status()?.bytes().await?;
        std::fs::write(dest, &bytes)?;
        Ok(())
    }
}