#!/usr/bin/env bash
set -euo pipefail

WORKDIR="/workspace/perf"
cd "${WORKDIR}"

BASE_URL="${SYSTEM_PERF_BASE_URL:-${TASK_SERVICE_BASE_URL:-}}"
if [[ -z "${BASE_URL}" ]]; then
  echo "SYSTEM_PERF_BASE_URL (or TASK_SERVICE_BASE_URL) is required." >&2
  exit 1
fi

echo "Preparing Maven dependencies..."
mvn -B -U dependency:go-offline >/dev/null

login_path="${SYSTEM_PERF_LOGIN_PATH:-/system/auth/login}"
username="${SYSTEM_PERF_USERNAME:-${TASK_SERVICE_USERNAME:-admin}}"
password="${SYSTEM_PERF_PASSWORD:-${TASK_SERVICE_PASSWORD:-admin}}"
event_id="${SYSTEM_PERF_EVENT_ID:-${TASK_SERVICE_EVENT_ID:-1}}"
task_id="${SYSTEM_PERF_TASK_ID:-${TASK_SERVICE_TASK_ID:-1}}"
upload_url="${SYSTEM_PERF_RESULTS_UPLOAD_URL:-${TASK_SERVICE_RESULTS_UPLOAD_URL:-}}"
results_object="${SYSTEM_PERF_RESULTS_OBJECT:-${TASK_SERVICE_RESULTS_OBJECT:-}}"

gatling_version="${GATLING_VERSION:-4.20.6}"

command=(
  mvn -B
  "io.gatling:gatling-maven-plugin:${gatling_version}:test"
  -Dgatling.skip=false
  -Dgatling.failOnAssertionFailure=false
  "-DtaskService.baseUrl=${BASE_URL}"
  "-DtaskService.loginPath=${login_path}"
  "-DtaskService.username=${username}"
  "-DtaskService.password=${password}"
  "-DtaskService.eventId=${event_id}"
  "-DtaskService.taskId=${task_id}"
)

echo "Starting Gatling with command: ${command[*]}"
set +e
"${command[@]}"
status=$?
set -e

RESULTS_DIR="${WORKDIR}/target/gatling"

if [[ -n "${upload_url}" ]] && [[ -d "${RESULTS_DIR}" ]]; then
  ARCHIVE="/tmp/gatling-results.tgz"
  echo "Archiving Gatling results..."
  tar -czf "${ARCHIVE}" -C "${RESULTS_DIR}" .
  echo "Uploading Gatling results to signed URL..."
  if curl -sS --fail -X PUT -T "${ARCHIVE}" -H "Content-Type: application/gzip" "${upload_url}"; then
    echo "Upload succeeded."
  else
    echo "Failed to upload Gatling results." >&2
    status=1
  fi
  rm -f "${ARCHIVE}"
fi

ARCHIVE_ROOT="/opt/gatling/results"
mkdir -p "${ARCHIVE_ROOT}"

if [[ -d "${RESULTS_DIR}" ]]; then
  RUN_ID="$(date -u +"%Y%m%dT%H%M%SZ")"
  RUN_RESULTS_DIR="${ARCHIVE_ROOT}/${RUN_ID}"
  mkdir -p "${RUN_RESULTS_DIR}"
  cp -R "${RESULTS_DIR}/." "${RUN_RESULTS_DIR}/"
  tar -czf "${ARCHIVE_ROOT}/${RUN_ID}.tgz" -C "${RUN_RESULTS_DIR}" .
  ln -sfn "${RUN_ID}.tgz" "${ARCHIVE_ROOT}/latest.tgz"
  printf '%s\n' "${RUN_ID}.tgz" > "${ARCHIVE_ROOT}/latest.txt"
else
  echo "Results directory ${RESULTS_DIR} not found; skipping archive."
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
