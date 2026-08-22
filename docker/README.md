# docker

This directory contains Docker examples for running PoloCloud.

Here you have different approaches to run PoloCloud:

- [Dockerfile Images](#dockerfile-images)
  - [Dockerfile - for deployment images](#dockerfile---for-deployment-images)
  - [dev.Dockerfile - for development images](#devdockerfile---for-development-images)
- [Compose Stacks](#compose-stacks)
  - [compose.yml - stack for deployment](#composeyml---stack-for-deployment)
  - [imageless.compose.yml - stack without Dockerfile](#imagelesscomposeyml---stack-without-dockerfile)
  - [dev.compose.yml - stack for development](#devcomposeyml---stack-for-development)
- [Host Permissions](#host-permissions)

## Dockerfile Images

### Dockerfile - for deployment images

Independent image that clones the repo itself from source to build PoloCloud:

- **Multi-stage Image:** build pipeline order: `clone` -> `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`
- **Clone Container:** Does not require the cloned repo on the host system

Copy the Dockerfile and use:

```bash
docker build -t polocloud .
```

### dev.Dockerfile - for development images

Less independent image that requires the already cloned repo at current location to build PoloCloud:

- **Multi-stage Image:** build pipeline order: `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`

Execute the following from the repository root directory:

```bash
docker build -t polocloud-dev -f docker/dev.Dockerfile .
```

## Compose Stacks

### compose.yml - stack for deployment

A simple Docker Compose stack using the Dockerfile image:

- **Compose Pipeline:** `polocloud-init` -> `polocloud`
- **Persistent Storage:** Persists local PoloCloud data in `./data`
- **Non-Root Execution:** See `Dockerfile` above.
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors

Copy the stack file and the `Dockerfile` and use:

```bash
docker compose up -d
```

### imageless.compose.yml - stack without Dockerfile

A Docker Compose stack using a container pipeline (instead of a multi-stage image) and cache to disk mount points:

- **Compose Pipeline:** `polocloud-init` -> `polocloud-cloner` -> `polocloud-builder` -> `polocloud`
- **Persistent Storage:** Persists local caches in `./cache` and PoloCloud data in `./data`
- **Non-Root Execution:** Runs as user ID `1000` to prevent running as `root` inside the container
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors
- **Cache Build:** Only build if missing, rebuild by deleting `./cache/polocloud/build`

Copy the stack file and use:

```bash
docker compose up -d
```

### dev.compose.yml - stack for development

A Docker Compose stack using the `dev.Dockerfile` image as base:

- **Compose Pipeline:** `polocloud-init` -> `polocloud-cloner` -> `polocloud-builder` -> `polocloud`
- **Persistent Storage:** Persists local caches in `./cache` and PoloCloud data in `./data`
- **Non-Root Execution:** See `dev.Dockerfile` above.
- **Init Container:** Fix ownership of mounted directories to avoid startup permission errors

Use the following in this `docker` dir of the repo:

```bash
docker compose up -d
```

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
