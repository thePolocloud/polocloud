use crate::polocloud::service_controller_client::ServiceControllerClient;
use crate::polocloud::{GroupType, ServiceFindRequest, ServiceFindResponse, ServiceState};
use crate::proto::group::Group;
use crate::proto::service::Service;
use tonic::transport::Channel;
use tonic::{Request, Response, Status};

pub struct ServiceProvider {
    service_stub: ServiceControllerClient<Channel>,
}

impl ServiceProvider {
    pub fn new(channel: Channel) -> Self {
        Self {
            service_stub: ServiceControllerClient::new(channel),
        }
    }

    pub async fn find_all_async(&mut self) -> Result<Vec<Service>, Status> {
        let request = Request::new(ServiceFindRequest::default());
        let response = self.service_stub.find(request).await?;
        let services = get_services_from_response(response);

        Ok(services)
    }
    pub async fn find_by_name_async(&mut self, name: String) -> Result<Vec<Service>, Status> {
        let request = Request::new(ServiceFindRequest {
            name: Some(name),
            group_name: None,
            r#type: None,
        });
        let response = self.service_stub.find(request).await?;
        let services = get_services_from_response(response);

        Ok(services)
    }

    pub async fn find_group_name_async(
        &mut self,
        group_name: String,
    ) -> Result<Vec<Service>, Status> {
        let request = Request::new(ServiceFindRequest {
            name: None,
            group_name: Some(group_name),
            r#type: None,
        });
        let response = self.service_stub.find(request).await?;
        let services = get_services_from_response(response);

        Ok(services)
    }

    pub async fn find_group_async(&mut self, group: Group) -> Result<Vec<Service>, Status> {
        let request = Request::new(ServiceFindRequest {
            name: None,
            group_name: Some(group.name().to_string()),
            r#type: None,
        });
        let response = self.service_stub.find(request).await?;
        let services = get_services_from_response(response);

        Ok(services)
    }
}

fn get_services_from_response(response: Response<ServiceFindResponse>) -> Vec<Service> {
    response
        .into_inner()
        .services
        .into_iter()
        .map(|proto_service| Service {
            group_name: proto_service.group_name,
            id: proto_service.id,
            hostname: proto_service.hostname,
            port: proto_service.port,
            state: ServiceState::try_from(proto_service.state).ok(),
            server_type: GroupType::try_from(proto_service.server_type)
                .unwrap_or(GroupType::Server),
            properties: proto_service.properties,
        })
        .collect()
}
