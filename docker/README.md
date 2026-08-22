# docker

This directory contains container utils tested using docker.
Here you have different approaches to run polocloud:

## TOC

- [TOC](#toc)
- [Dockerfile - image for development](#dockerfile---image-for-development)
- [compose.yml - stack for development](#composeyml---stack-for-development)
- [source.Dockerfile - image for deployment](#sourcedockerfile---image-for-deployment)
- [source.compose.yml - stack for deployment](#sourcecomposeyml---stack-for-deployment)
- [imageless.compose.yml - stack without Dockerfile](#imagelesscomposeyml---stack-without-dockerfile)
- [Host Permissions](#host-permissions)

## Dockerfile - image for development

Requires the already cloned repo at current location to build polocloud:

- **Multistage Image:** build pipeline order: `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`

```bash
docker build -t polocloud -f docker/Dockerfile .
```

## compose.yml - stack for development

A Docker Compose stack using the `Dockerfile`-image as base:

- **Compose Pipeline:** `polocloud-init` -> `polocloud-cloner` -> `polocloud-builder` -> `polocloud`
- **Persistent Storage:** Persists local caches in `./cache` and polocloud data in `./data`
- **Non-Root Execution:** See `Dockerfile` above.
- **Init Container:** Align mounts file rights for permission startup errors

## source.Dockerfile - image for deployment

More independent image that clones the repo from source to build polocloud:

- **Multistage Image:** build pipeline order: `clone` -> `build` -> `runtime`
- **Non-Root Execution:** Runs as `non-root` user with ID `1000`
- **Clone Container:** Does not require the cloned repo on the host system

```bash
docker build -t polocloud-source -f docker/source.Dockerfile .
```

## source.compose.yml - stack for deployment

A Docker Compose stack using the `source.Dockerfile`-image as base:

- **Compose Pipeline:** `polocloud-init` -> `polocloud`
- **Persistent Storage:** Persists local polocloud data in `./data`
- **Non-Root Execution:** See `source.Dockerfile` above.
- **Init Container:** Align mounts file rights for permission startup errors

## imageless.compose.yml - stack without Dockerfile

A Docker Compose stack using a container pipeline (instead of a multistage image) and cache to disk mountpoints:

- **Compose Pipeline:** `polocloud-init` -> `polocloud-cloner` -> `polocloud-builder` -> `polocloud`
- **Persistent Storage:** Persists local caches in `./cache` and polocloud data in `./data`
- **Non-Root Execution:** Runs as user ID `1000` to prevent running as `root` inside the container
- **Init Container:** Align mounts file rights for permission startup errors
- **Cache Build:** Only build if missing, rebuild by deleting `./cache/polocloud/build`

## Host Permissions

If you are having trouble setting permissions for your host system, give your user a group with ID `1000` so that you can access and edit the files:

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
  sudo usermod -aG 1000 "$USER"
  ```
