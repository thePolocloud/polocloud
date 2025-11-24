use std::env;
use tonic::transport::{Channel, Endpoint};

pub struct SdkGrpcClient {
    pub channel: Option<Channel>,
}

impl SdkGrpcClient {
    pub async fn new() -> Result<Self, Box<dyn std::error::Error>> {
        let agent_port = env::var("agent_port")
            .unwrap_or_else(|_| "8932".to_string())
            .parse::<u16>()?;

        let endpoint = Endpoint::from_shared(format!("http://127.0.0.1:{}", agent_port))?;
        let channel = endpoint.connect().await?;

        Ok(Self {
            channel: Some(channel),
        })
    }
}
