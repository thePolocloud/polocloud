> ⚠️ **Warning** This SDK is in early development and our team has limited Rust experience, so you may encounter some
> unusual implementations. If you find anything that could be improved, please create an issue and let us know how to make
> it better!

# Polocloud Rust SDK
A Rust SDK for interacting with the Polocloud Minecraft cloud system. This SDK provides a simple interface for services to communicate with the cloud infrastructure through gRPC.

## Overview

The Polocloud SDK allows Minecraft services to:
- Query and manage services within the cloud
- Retrieve group information and configurations
- Subscribe to real-time events from the cloud system

## Quick Start

### Dependency

Add SDK to you project by adding this to your `Cargo.toml`:

```toml
[dependencies]
tokio = { version = "1.47.0", features = ["macros", "rt-multi-thread"] }
polocloud-sdk = { git = "https://github.com/HttpMarco/polocloud.git", version = "3.0.0-pre.6.1-SNAPSHOT" }
```

### Connection Details

The SDK connects to the Polocloud agent via gRPC. The connection details are passed via environment variables automatically by the cloud.

### Basic Usage

```rust
use polocloud_sdk::Polocloud;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Get the singleton instance
    let polocloud = Polocloud::instance().await;
    
    // Check if the connection is available
    let is_available = {
        let client = polocloud.lock().unwrap();
        client.available()
    };
    
    if is_available {
        println!("Successfully connected to Polocloud!");
    }
    
    Ok(())
}
```

## Configuration

The SDK uses environment variables for configuration:

- `agent_port` - Port of the Polocloud agent
- `service-name` - Name of the current service

This variables are set by the cloud so you don't need to set them your self.

## Core Components

### Services

Query and retrieve information about running services in the cloud.

```rust
// Get all services
let service_provider = polocloud.service_provider();
let mut provider = service_provider.lock().unwrap();
let services = provider.find_async().await?;

for service in services {
    println!("Service: {} ({}:{})", service.group_name, service.hostname, service.port);
    println!("State: {:?}", service.state);
    println!("Type: {:?}", service.server_type);
}
```

#### Service Properties

- `group_name` - Name of the group this service belongs to
- `id` - Unique service identifier
- `hostname` - Service hostname
- `port` - Service port
- `state` - Current service state (Optional)
- `server_type` - Type of server (Server, Proxy, etc.)
- `properties` - Additional service properties

### Groups

Manage and query server groups and their configurations.

```rust
// Get all groups
let group_provider = polocloud.group_provider();
let mut provider = group_provider.lock().unwrap();
let groups = provider.find_async().await?;

for group in groups {
    println!("Group: {}", group.name());
    println!("Memory: {} - {} MB", group.minimum_memory(), group.maximum_memory());
    println!("Online: {} - {} services", group.minimum_online(), group.maximum_online());
}

// Find specific group by name
let group = provider.find_by_name_async("lobby".to_string()).await?;
if let Some(group) = group {
    println!("Found group: {}", group.name());
}
```

### Events

Subscribe to real-time events from the cloud system.

```rust
// Subscribe to events
let event_provider = polocloud.event_provider();
let mut provider = event_provider.lock().unwrap();

provider.subscribe(|event: ServiceShutdownEvent| {
    println!("Service shut down");
}).await?;
```

### Checking Connection Status

```rust
let polocloud = Polocloud::instance().await;
let client = polocloud.lock().unwrap();

if client.available() {
    println!("Connected to Polocloud agent");
    println!("Service name: {}", client.self_service_name());
} else {
    println!("Not connected to Polocloud agent");
}
```
