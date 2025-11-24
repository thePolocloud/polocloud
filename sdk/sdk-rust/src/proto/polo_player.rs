use crate::polocloud::PlayerSnapshot;

#[derive(Debug, Clone)]
pub struct PolocloudPlayer {
    name: String,
    uuid: String,
    curren_service_name: String,
}

impl PolocloudPlayer {
    pub fn new(name: String, uuid: String, curren_service_name: String) -> Self {
        Self {
            name,
            uuid,
            curren_service_name,
        }
    }

    pub fn name(&self) -> &str {
        &self.name
    }

    pub fn uuid(&self) -> &str {
        &self.uuid
    }

    pub fn curren_service_name(&self) -> &str {
        &self.curren_service_name
    }

    pub fn from_snapshot(snapshot: PlayerSnapshot) -> Self {
        Self {
            name: snapshot.name,
            uuid: snapshot.unique_id,
            curren_service_name: snapshot.current_service_name,
        }
    }
}
