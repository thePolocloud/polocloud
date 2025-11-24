use crate::polocloud::group_controller_client::GroupControllerClient;
use crate::polocloud::FindGroupRequest;
use crate::proto::group::Group;
use tonic::transport::Channel;
use tonic::{Request, Status};

pub struct GroupProvider {
    group_stub: GroupControllerClient<Channel>,
}

impl GroupProvider {
    pub fn new(channel: Channel) -> Self {
        Self {
            group_stub: GroupControllerClient::new(channel),
        }
    }

    pub async fn find_all_async(&mut self) -> Result<Vec<Group>, Status> {
        let request = Request::new(FindGroupRequest::default());
        let response = self.group_stub.find(request).await?;

        let groups = response
            .into_inner()
            .groups
            .into_iter()
            .map(Group::new)
            .collect();

        Ok(groups)
    }

    pub async fn find_by_name_async(&mut self, name: String) -> Result<Option<Group>, Status> {
        let request = Request::new(FindGroupRequest { name, r#type: 0 });
        let response = self.group_stub.find(request).await?;

        let group = response
            .into_inner()
            .groups
            .into_iter()
            .map(Group::new)
            .next();

        Ok(group)
    }
}
