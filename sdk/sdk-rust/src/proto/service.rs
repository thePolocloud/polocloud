use crate::polocloud::{GroupType, ServiceState};
use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, Clone, Deserialize)]
pub struct Service {
    #[serde(rename(deserialize = "groupName"))]
    pub group_name: String,
    pub id: u32,
    pub hostname: String,
    pub port: u32,
    pub state: Option<ServiceState>,
    #[serde(rename(deserialize = "type"))]
    pub server_type: GroupType,
    pub properties: HashMap<String, String>,
}
