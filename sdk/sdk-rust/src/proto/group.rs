use crate::polocloud::GroupSnapshot;
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct Group {
    snapshot: GroupSnapshot,
}

impl Group {
    pub fn new(snapshot: GroupSnapshot) -> Self {
        Self { snapshot }
    }

    pub fn name(&self) -> &str {
        &self.snapshot.name
    }

    pub fn minimum_memory(&self) -> i32 {
        self.snapshot.minimum_memory
    }

    pub fn maximum_memory(&self) -> i32 {
        self.snapshot.maximum_memory
    }

    pub fn minimum_online(&self) -> i32 {
        self.snapshot.minimum_online
    }

    pub fn maximum_online(&self) -> i32 {
        self.snapshot.maximum_online
    }

    pub fn properties(&self) -> &HashMap<String, String> {
        &self.snapshot.properties
    }
}
