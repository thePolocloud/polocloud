use crate::provider::event::EventProvider;
use crate::provider::group::GroupProvider;
use crate::provider::player::PlayerProvider;
use crate::provider::service::ServiceProvider;
use crate::rpc::rpc_client::SdkGrpcClient;
use std::{env, sync::Mutex};
use tokio::sync::OnceCell;
use tonic::transport::Channel;

pub struct Polocloud {
    grpc_client: SdkGrpcClient,
    grpc_channel: Channel,

    event_provider: Mutex<EventProvider>,
    group_provider: Mutex<GroupProvider>,
    player_provider: Mutex<PlayerProvider>,
    service_provider: Mutex<ServiceProvider>,
}

impl Polocloud {
    pub async fn new() -> Result<Self, Box<dyn std::error::Error>> {
        let grpc_client = SdkGrpcClient::new().await?;
        let grpc_channel = grpc_client.channel.clone().ok_or("No channel found")?;

        let service_name =
            env::var("service-name").unwrap_or_else(|_| "default-service".to_string());

        let event_provider = Mutex::new(EventProvider::new(grpc_channel.clone(), service_name));
        let player_provider = Mutex::new(PlayerProvider::new(grpc_channel.clone()));
        let group_provider = Mutex::new(GroupProvider::new(grpc_channel.clone()));
        let service_provider = Mutex::new(ServiceProvider::new(grpc_channel.clone()));
        Ok(Self {
            grpc_client,
            grpc_channel,
            event_provider,
            group_provider,
            player_provider,
            service_provider,
        })
    }

    pub fn event_provider(&self) -> &Mutex<EventProvider> {
        &self.event_provider
    }

    pub fn group_provider(&self) -> &Mutex<GroupProvider> {
        &self.group_provider
    }

    pub fn player_provider(&self) -> &Mutex<PlayerProvider> {
        &self.player_provider
    }

    pub fn service_provider(&self) -> &Mutex<ServiceProvider> {
        &self.service_provider
    }
}

static INSTANCE: OnceCell<Polocloud> = OnceCell::const_new();

pub async fn polocloud() -> Result<&'static Polocloud, Box<dyn std::error::Error>> {
    INSTANCE
        .get_or_try_init(|| async { Polocloud::new().await })
        .await
}
