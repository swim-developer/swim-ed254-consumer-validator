REGISTRY   ?= quay.io/masales
TAG        ?= latest
PLATFORMS   := linux/amd64,linux/arm64
MVN_NATIVE := -Dnative -DskipTests \
              -Dquarkus.native.container-build=true \
              -Dquarkus.native.container-runtime=podman
IMAGE_NAME := swim-ed254-consumer-validator

.PHONY: help jvm native-amd64 native-arm64 manifest push native

help:
	@echo ""
	@echo "  swim-ed254-consumer-validator — available targets"
	@echo "  ─────────────────────────────────────────────────────────"
	@echo ""
	@echo "    jvm             JVM multi-arch image — build + push"
	@echo "    native-amd64    Native amd64 image — build + push  (run on amd64)"
	@echo "    native-arm64    Native arm64 image — build + push  (run on arm64)"
	@echo "    manifest        Create multi-arch manifest from registry images"
	@echo "    push            Push manifest to registry"
	@echo "    native          Full native sequence (amd64 + arm64 + manifest + push)"
	@echo ""
	@echo "  Pre-requisite: shared modules must be installed in local Maven repo."
	@echo "  Run once from swim-developer-validators: ./mvnw clean install -DskipTests"
	@echo ""
	@echo "  Variables: REGISTRY=$(REGISTRY)  TAG=$(TAG)"

jvm:
	-podman rmi  $(REGISTRY)/$(IMAGE_NAME):$(TAG) 2>/dev/null
	-podman manifest rm $(REGISTRY)/$(IMAGE_NAME):$(TAG) 2>/dev/null
	podman manifest create $(REGISTRY)/$(IMAGE_NAME):$(TAG)
	./mvnw clean package -DskipTests
	podman build --no-cache --platform $(PLATFORMS) \
		-f src/main/docker/Containerfile.jvm \
		--manifest $(REGISTRY)/$(IMAGE_NAME):$(TAG) .
	podman manifest push --all $(REGISTRY)/$(IMAGE_NAME):$(TAG) \
		docker://$(REGISTRY)/$(IMAGE_NAME):$(TAG)
	@echo "Pushed: $(REGISTRY)/$(IMAGE_NAME):$(TAG)  (JVM multi-arch)"

native-amd64:
	./mvnw clean package $(MVN_NATIVE) \
		-Dquarkus.native.container-runtime-options=--platform,linux/amd64
	podman build --no-cache --platform linux/amd64 \
		-f src/main/docker/Containerfile.native-micro \
		-t $(REGISTRY)/$(IMAGE_NAME):$(TAG)-amd64 .
	podman push $(REGISTRY)/$(IMAGE_NAME):$(TAG)-amd64
	@echo "Pushed: $(REGISTRY)/$(IMAGE_NAME):$(TAG)-amd64"

native-arm64:
	./mvnw clean package $(MVN_NATIVE) \
		-Dquarkus.native.container-runtime-options=--platform,linux/arm64
	podman build --no-cache --platform linux/arm64 \
		-f src/main/docker/Containerfile.native-micro \
		-t $(REGISTRY)/$(IMAGE_NAME):$(TAG)-arm64 .
	podman push $(REGISTRY)/$(IMAGE_NAME):$(TAG)-arm64
	@echo "Pushed: $(REGISTRY)/$(IMAGE_NAME):$(TAG)-arm64"

manifest:
	-podman rmi  $(REGISTRY)/$(IMAGE_NAME):$(TAG) 2>/dev/null
	-podman manifest rm $(REGISTRY)/$(IMAGE_NAME):$(TAG) 2>/dev/null
	podman manifest create $(REGISTRY)/$(IMAGE_NAME):$(TAG) \
		docker://$(REGISTRY)/$(IMAGE_NAME):$(TAG)-amd64 \
		docker://$(REGISTRY)/$(IMAGE_NAME):$(TAG)-arm64
	@echo "Manifest ready: $(REGISTRY)/$(IMAGE_NAME):$(TAG)"

push:
	podman manifest push --all $(REGISTRY)/$(IMAGE_NAME):$(TAG) \
		docker://$(REGISTRY)/$(IMAGE_NAME):$(TAG)
	@echo "Pushed: $(REGISTRY)/$(IMAGE_NAME):$(TAG)  (multi-arch)"

native: native-amd64 native-arm64 manifest push
