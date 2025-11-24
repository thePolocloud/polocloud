pub mod polocloud {
    tonic::include_proto!("dev.httpmarco.polocloud.v1.proto");
}

pub mod event;
pub mod init;
pub mod proto;
mod provider;
pub mod rpc;

#[cfg(test)]
pub mod test {
    use crate::event::model::Event;
    use crate::event::model::ServiceOnlineEvent;
    use crate::init::polocloud;

    #[test]
    fn test_event_name_macro() {
        println!(
            "Macro created event name: {}",
            ServiceOnlineEvent::event_name()
        );
    }

    #[tokio::test]
    async fn test_polocloud_group_provider() {
        let group_provider = polocloud().await.unwrap().group_provider();

        group_provider
            .lock()
            .unwrap()
            .find_all_async()
            .await
            .iter()
            .for_each(|group| {
                println!("Groups: {:?}", group);
            });
    }

    #[tokio::test]
    async fn test_polocloud_service_provider() {
        let service_provider = polocloud().await.unwrap().service_provider();

        service_provider
            .lock()
            .unwrap()
            .find_all_async()
            .await
            .iter()
            .for_each(|service| {
                println!("Service: {:?}", service);
            });
    }
}
