<div align="center">

# ☁️+🐋 PoloCloud in Docker

You can now securely deploy PoloCloud using Docker! This directory contains examples for your deployment.

</div>

Here you have different approaches to run PoloCloud:

- [Dockerfile Images](#dockerfile-images)
  - [Dockerfile - for deployment images](#dockerfile---for-deployment-images)
  - [dev.Dockerfile - for development images](#devdockerfile---for-development-images)
- [Compose Stacks](#compose-stacks)
  - [compose.yml - stack for deployment](#composeyml---stack-for-deployment)
  - [imageless.compose.yml - stack without Dockerfile](#imagelesscomposeyml---stack-without-dockerfile)
  - [dev.compose.yml - stack for development](#devcomposeyml---stack-for-development)
- [Host Permissions](#host-permissions)
- [Customization](#customization)
  - [Data \& Cache Mounts](#data--cache-mounts)
  - [Service Separation](#service-separation)
  - [Issues?](#issues)
  - [🤝 Community](#-community)

## Dockerfile Images

### Dockerfile - for deployment images

Independent image that clones the repo itself from source to build PoloCloud:

- **Multi-stage Image:** build pipeline order: `clone` -> `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`
- **Clone Container:** Does not require the cloned repo on the host system

Copy the Dockerfile and use:

```bash
docker build -t polocloud .
docker run -it --rm -v ./data/polocloud:/data polocloud
```

Or from the repository root:

```bash
docker build -t polocloud -f docker/Dockerfile .
docker run -it --rm -v ./docker/data/polocloud:/data polocloud
```

### dev.Dockerfile - for development images

Less independent image that requires the already cloned repo at current location to build PoloCloud:

- **Multi-stage Image:** build pipeline order: `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`

Execute the following from the repository root directory:

```bash
docker build -t polocloud-dev -f docker/dev.Dockerfile .
docker run -it --rm -v ./docker/data/polocloud:/data polocloud-dev
```

## Compose Stacks

### compose.yml - stack for deployment

A simple Docker Compose stack using the Dockerfile image:

- **Compose Pipeline:** `polocloud-init` -> `polocloud`
- **Persistent Storage:** Persists local PoloCloud data in `./data`
- **Non-Root Execution:** See `Dockerfile` above.
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors

Copy the stack file and the `Dockerfile` into one directory and use:

```bash
docker compose up -d
```

Or from the repository root:

```bash
docker compose -f docker/compose.yml up -d
```

Rebuild after image changes with `docker compose up -d --build`. Attach to the console with `docker attach $(docker compose ps -q polocloud)`.

### imageless.compose.yml - stack without Dockerfile

A Docker Compose stack using a container pipeline (instead of a multi-stage image) and cache to disk mount points:

- **Compose Pipeline:** `polocloud-init` -> `polocloud-cloner` -> `polocloud-builder` -> `polocloud`
- **Persistent Storage:** Persists local caches in `./cache` and PoloCloud data in `./data`
- **Non-Root Execution:** Runs as user ID `1000` to prevent running as `root` inside the container
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors
- **Cache Build:** Only build if missing, rebuild by deleting `./cache/polocloud/build`

Copy the stack file, rename it to `compose.yml` if needed, and use:

```bash
docker compose up -d
```

Or from the repository root:

```bash
docker compose -f docker/imageless.compose.yml up -d
```

Attach to the console with `docker attach $(docker compose -f imageless.compose.yml ps -q polocloud)`.

### dev.compose.yml - stack for development

A Docker Compose stack using the `dev.Dockerfile` image as base:

- **Compose Pipeline:** `polocloud-init` -> `polocloud`
- **Persistent Storage:** Persists local PoloCloud data in `./data`
- **Non-Root Execution:** See `dev.Dockerfile` above.
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors

Use the following in the repository root:

```bash
docker compose -f docker/dev.compose.yml up -d
```

Rebuild after source changes with `--build`. Attach to the console with `docker attach $(docker compose -f dev.compose.yml ps -q polocloud)`.

## Host Permissions

If you are having trouble with host file permissions, give your user a group with ID `1000` so that you can access and edit the files:

- Check if group already exists:
  ```bash
  getent group 1000
  ```
- Create group if missing:
  ```bash
  sudo groupadd -g 1000 docker-access
  ```
- Add group to current user:
  ```bash
  sudo usermod -aG docker-access "$USER"
  ```
- Re-login so the new group applies, or use:
  ```bash
  newgrp docker-access
  ```

## Customization

The compose stacks are designed for customization.

### Data & Cache Mounts

All stacks mount PoloCloud data at `./data/polocloud/`. The imageless stack also mounts build caches at `./cache/polocloud/`.

You can use sibling directories under `./data/*/` (and `./cache/*/` for imageless) for your own extra services like databases for your Minecraft plugins.

You can use `./data/perms/` for your permission system and add a database container to the stack that mounts it.

### Service Separation

Using Docker Compose `include` statements you can separate your extra service containers from the PoloCloud services.

Create a `compose.yml` that then imports the seperated compose parts:

```
include:
  - databases.compose.yml
  - plugin-builder.compose.yml
  - polocloud.compose.yml
```

<div align="center">

#

### Issues?

If you have any issues with the Docker setup you're welcome to create an issue or join the Discord server and ask your questions there.

### 🤝 Community

<a href="https://discord.polocloud.de">
    <img alt="PoloCloud Discord" src="https://discord.com/api/guilds/1278460874679386244/widget.png?style=banner2">
</a>

</div>
