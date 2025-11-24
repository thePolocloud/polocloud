use crate::event::model::Event;
use crate::polocloud::event_provider_client::EventProviderClient;
use crate::polocloud::EventSubscribeRequest;
use tonic::codegen::tokio_stream::StreamExt;
use tonic::transport::Channel;
use tonic::{Request, Status};

pub struct EventProvider {
    event_stub: EventProviderClient<Channel>,
    service_name: String,
}

impl EventProvider {
    pub fn new(channel: Channel, service_name: String) -> Self {
        Self {
            event_stub: EventProviderClient::new(channel),
            service_name,
        }
    }

    pub async fn subscribe<T, F>(&mut self, mut callback: F) -> Result<(), Status>
    where
        T: Event + serde::de::DeserializeOwned + Send + 'static,
        F: FnMut(T) + Send + 'static,
    {
        let request = Request::new(EventSubscribeRequest {
            service_name: self.service_name.clone(),
            event_name: T::event_name().to_string(),
        });

        let mut event_stub = self.event_stub.clone();

        tokio::spawn(async move {
            let mut stream = match event_stub.subscribe(request).await {
                Ok(s) => s.into_inner(),
                Err(e) => {
                    eprintln!("Error subscribing to event: {}", e);
                    return;
                }
            };
            while let Some(event_context_result) = stream.next().await {
                match event_context_result {
                    Ok(event_context) => {
                        match serde_json::from_str::<T>(&event_context.event_data) {
                            Ok(event) => callback(event),
                            Err(e) => eprintln!("Error deserializing event: {}", e),
                        }
                    }
                    Err(e) => {
                        eprintln!("Error while subscribing to event: {}", e);
                        break;
                    }
                }
            }
        });

        Ok(())
    }
}
