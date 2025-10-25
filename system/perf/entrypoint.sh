#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${SYSTEM_PERF_BASE_URL:-${TASK_SERVICE_PERF_BASE_URL:-${TASK_SERVICE_BASE_URL:-}}}"
if [[ -z "${BASE_URL}" ]]; then
  echo "SYSTEM_PERF_BASE_URL (or TASK_SERVICE_PERF_BASE_URL/TASK_SERVICE_BASE_URL) is required" >&2
  exit 1
fi

cd /workspace

export MAVEN_OPTS="${MAVEN_OPTS:--Xms256m -Xmx512m -XX:+UseContainerSupport}"

echo "PWD=$(pwd)"
echo "Listing modules at repo root:"
ls -la || true

echo "Preparing local Maven repository for system module..."
mvn -B -U -DskipTests install

declare -a args
args=(
  mvn -B -f system/pom.xml -am \
    io.gatling:gatling-maven-plugin:4.20.6:test \
    -Dgatling.skip=false \
    -Dgatling.failOnAssertionFailure=false \
    -DtaskService.baseUrl="${BASE_URL}"
)

login_path="${SYSTEM_PERF_LOGIN_PATH:-${TASK_SERVICE_PERF_LOGIN_PATH:-${TASK_SERVICE_LOGIN_PATH:-}}}"
username="${SYSTEM_PERF_USERNAME:-${TASK_SERVICE_PERF_USERNAME:-${TASK_SERVICE_USERNAME:-}}}"
password="${SYSTEM_PERF_PASSWORD:-${TASK_SERVICE_PERF_PASSWORD:-${TASK_SERVICE_PASSWORD:-}}}"
event_id="${SYSTEM_PERF_EVENT_ID:-${TASK_SERVICE_PERF_EVENT_ID:-${TASK_SERVICE_EVENT_ID:-}}}"
task_id="${SYSTEM_PERF_TASK_ID:-${TASK_SERVICE_PERF_TASK_ID:-${TASK_SERVICE_TASK_ID:-}}}"
upload_url="${SYSTEM_PERF_RESULTS_UPLOAD_URL:-${TASK_SERVICE_RESULTS_UPLOAD_URL:-}}"
results_object="${SYSTEM_PERF_RESULTS_OBJECT:-${TASK_SERVICE_RESULTS_OBJECT:-}}"

if [[ -n "${login_path}" ]]; then
  args+=("-DtaskService.loginPath=${login_path}")
fi
if [[ -n "${username}" ]]; then
  args+=("-DtaskService.username=${username}")
fi
if [[ -n "${password}" ]]; then
  args+=("-DtaskService.password=${password}")
fi
if [[ -n "${event_id}" ]]; then
  args+=("-DtaskService.eventId=${event_id}")
fi
if [[ -n "${task_id}" ]]; then
  args+=("-DtaskService.taskId=${task_id}")
fi

echo "Starting Gatling with command: ${args[*]}"
set +e
"${args[@]}"
status=$?
set -e

RESULTS_DIR="/workspace/system/target/gatling"

# Upload results when signed URL is provided.
if [[ -n "${upload_url}" ]]; then
  if [[ -d "${RESULTS_DIR}" ]]; then
    ARCHIVE="/tmp/gatling-results.tgz"
    echo "Archiving Gatling results from ${RESULTS_DIR}..."
    tar -czf "${ARCHIVE}" -C "${RESULTS_DIR}" .
    echo "Uploading Gatling results archive to signed URL."
    if curl -sS --fail -X PUT -T "${ARCHIVE}" -H "Content-Type: application/gzip" "${upload_url}"; then
      echo "Upload succeeded."
    else
      echo "Failed to upload Gatling results to signed URL." >&2
      status=1
    fi
    rm -f "${ARCHIVE}"
  else
    echo "Results directory ${RESULTS_DIR} not found; skipping upload."
  fi
fi

ARCHIVE_ROOT="/opt/gatling/results"
mkdir -p "${ARCHIVE_ROOT}"

RUN_ID="$(date -u +"%Y%m%dT%H%M%SZ")"
RUN_RESULTS_DIR="${ARCHIVE_ROOT}/${RUN_ID}"

if [[ -d "${RESULTS_DIR}" ]]; then
  echo "Syncing Gatling results into ${RUN_RESULTS_DIR}..."
  mkdir -p "${RUN_RESULTS_DIR}"
  cp -R "${RESULTS_DIR}/." "${RUN_RESULTS_DIR}/"
  tar -czf "${ARCHIVE_ROOT}/${RUN_ID}.tgz" -C "${RUN_RESULTS_DIR}" .
  ln -sfn "$(basename "${ARCHIVE_ROOT}/${RUN_ID}.tgz")" "${ARCHIVE_ROOT}/latest.tgz"
  printf '%s\n' "$(basename "${ARCHIVE_ROOT}/${RUN_ID}.tgz")" > "${ARCHIVE_ROOT}/latest.txt"
else
  echo "Results directory ${RESULTS_DIR} not found; no local archive created."
fi

if [[ -n "${results_object}" ]]; then
  echo "Results object key: ${results_object}"
fi

if [[ "${status}" -eq 0 ]]; then
  echo "GATLING_RESULT=success"
else
  echo "GATLING_RESULT=failure"
fi

keep_alive="${GATLING_KEEP_ALIVE:-true}"

if [[ "${keep_alive}" == "true" ]]; then
  echo "Gatling finished with status ${status}. Keeping container alive (GATLING_KEEP_ALIVE=${keep_alive})."
  tail -f /dev/null &
  child=$!
  trap "kill ${child}" TERM INT
  wait "${child}"
else
  exit "${status}"
fi
