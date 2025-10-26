#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/chronoflow-perf}"
RESULTS_ROOT="${RESULTS_ROOT:-/opt/gatling/results}"
GIT_REMOTE="${GIT_REMOTE:-origin}"
GIT_REF="${GIT_REF:-main}"
IMAGE_NAME="${IMAGE_NAME:-chronoflow-gatling:latest}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-perf/Dockerfile}"
CONTAINER_RESULTS_PATH="/opt/gatling/results"
CONTAINER_M2_PATH="/root/.m2"
M2_CACHE_DIR="${M2_CACHE_DIR:-$HOME/.m2}"
DOCKER_NETWORK_MODE="${DOCKER_NETWORK_MODE:-}"

mkdir -p "${RESULTS_ROOT}"
mkdir -p "${M2_CACHE_DIR}"

if [[ ! -d "${PROJECT_DIR}/.git" ]]; then
  if [[ -n "${GIT_CLONE_URL:-}" ]]; then
    if [[ "${PROJECT_DIR}" == "/" ]]; then
      echo "Refusing to remove PROJECT_DIR '/'." >&2
      exit 1
    fi
    rm -rf "${PROJECT_DIR}"
    git clone --branch "${GIT_REF}" --depth 1 --single-branch "${GIT_CLONE_URL}" "${PROJECT_DIR}"
  else
    echo "PROJECT_DIR ${PROJECT_DIR} is not a git repository. Set GIT_CLONE_URL to allow cloning." >&2
    exit 1
  fi
fi

cd "${PROJECT_DIR}"

if [[ "${SKIP_GIT_SYNC:-false}" != "true" ]]; then
  git fetch "${GIT_REMOTE}" "${GIT_REF}"
  git reset --hard "${GIT_REMOTE}/${GIT_REF}"
else
  echo "Skipping git fetch/reset because SKIP_GIT_SYNC=${SKIP_GIT_SYNC}"
fi

if [[ "${SKIP_IMAGE_BUILD:-false}" != "true" ]]; then
  docker build -f "${DOCKERFILE_PATH}" -t "${IMAGE_NAME}" .
else
  echo "Skipping Docker image build because SKIP_IMAGE_BUILD=${SKIP_IMAGE_BUILD}"
fi

RUN_ID="$(date -u +"%Y%m%dT%H%M%SZ")"
RUN_RESULTS_DIR="${RESULTS_ROOT}/${RUN_ID}"
mkdir -p "${RUN_RESULTS_DIR}"

declare -a docker_args
docker_args=(docker run --rm --name "gatling-${RUN_ID}")

if [[ -n "${DOCKER_NETWORK_MODE}" ]]; then
  docker_args+=("--network" "${DOCKER_NETWORK_MODE}")
fi

BASE_URL="${SYSTEM_PERF_BASE_URL:-${TASK_SERVICE_BASE_URL:-}}"
if [[ -z "${BASE_URL}" ]]; then
  echo "SYSTEM_PERF_BASE_URL (or TASK_SERVICE_BASE_URL) is required." >&2
  exit 1
fi

docker_args+=("-e" "SYSTEM_PERF_BASE_URL=${BASE_URL}")
docker_args+=("-e" "GATLING_KEEP_ALIVE=false")

for var in SYSTEM_PERF_LOGIN_PATH SYSTEM_PERF_USERNAME SYSTEM_PERF_PASSWORD SYSTEM_PERF_EVENT_ID SYSTEM_PERF_TASK_ID SYSTEM_PERF_RESULTS_UPLOAD_URL SYSTEM_PERF_RESULTS_OBJECT TASK_SERVICE_LOGIN_PATH TASK_SERVICE_USERNAME TASK_SERVICE_PASSWORD TASK_SERVICE_EVENT_ID TASK_SERVICE_TASK_ID TASK_SERVICE_RESULTS_UPLOAD_URL TASK_SERVICE_RESULTS_OBJECT; do
  if [[ -n "${!var:-}" ]]; then
    docker_args+=("-e" "${var}=${!var}")
  fi
done

docker_args+=("-v" "${RUN_RESULTS_DIR}:${CONTAINER_RESULTS_PATH}")
docker_args+=("-v" "${M2_CACHE_DIR}:${CONTAINER_M2_PATH}")
docker_args+=("${IMAGE_NAME}")

echo "Starting Gatling container: ${docker_args[*]}"
set +e
"${docker_args[@]}"
status=$?
set -e

ARCHIVE_PATH="${RESULTS_ROOT}/${RUN_ID}.tgz"
if [[ -d "${RUN_RESULTS_DIR}" ]]; then
  tar -czf "${ARCHIVE_PATH}" -C "${RUN_RESULTS_DIR}" .
else
  echo "Warning: Expected results directory ${RUN_RESULTS_DIR} not found; creating empty archive."
  tar -czf "${ARCHIVE_PATH}" -T /dev/null
fi

ln -sfn "$(basename "${ARCHIVE_PATH}")" "${RESULTS_ROOT}/latest.tgz"
printf '%s\n' "$(basename "${ARCHIVE_PATH}")" > "${RESULTS_ROOT}/latest.txt"

echo "Gatling run ${RUN_ID} completed with status ${status}. Archive stored at ${ARCHIVE_PATH}."

exit "${status}"
