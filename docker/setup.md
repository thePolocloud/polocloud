### Docker Setup

Build (native, for your current architecture):
```bash
docker build -t polocloud:development -f docker/Dockerfile .
```

Run (DockerRuntime requires host Docker socket):
```bash
docker rm -f polocloud 2>/dev/null || true
docker run -d --name polocloud \
	-e DOCKER_HOST=unix:///var/run/docker.sock \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-p 8932:8932 \
	polocloud:development
```

Multi-arch build (amd64 + arm64) using buildx (push required for multi-platform):
```bash
docker buildx create --use --name polocloud-builder 2>/dev/null || docker buildx use polocloud-builder
docker buildx build \
	--platform linux/amd64,linux/arm64 \
	-t ghcr.io/<org>/<repo>:<tag> \
	-f docker/Dockerfile \
	--push \
	.
```

