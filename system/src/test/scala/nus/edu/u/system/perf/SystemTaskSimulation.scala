package nus.edu.u.system.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Gatling simulation for ChronoFlow task APIs deployed on Railway.
 *
 * Configuration keys (system property or environment variable):
 * - `taskService.baseUrl` / `SYSTEM_PERF_BASE_URL` / `TASK_SERVICE_PERF_BASE_URL` / `TASK_SERVICE_BASE_URL`
 * - `taskService.loginPath` / `SYSTEM_PERF_LOGIN_PATH` / `TASK_SERVICE_PERF_LOGIN_PATH` / `TASK_SERVICE_LOGIN_PATH`
 * - `taskService.username` / `SYSTEM_PERF_USERNAME` / `TASK_SERVICE_PERF_USERNAME` / `TASK_SERVICE_USERNAME`
 * - `taskService.password` / `SYSTEM_PERF_PASSWORD` / `TASK_SERVICE_PERF_PASSWORD` / `TASK_SERVICE_PASSWORD`
 * - `taskService.eventId` / `SYSTEM_PERF_EVENT_ID` / `TASK_SERVICE_PERF_EVENT_ID` / `TASK_SERVICE_EVENT_ID`
 * - `taskService.taskId` / `SYSTEM_PERF_TASK_ID` / `TASK_SERVICE_PERF_TASK_ID` / `TASK_SERVICE_TASK_ID`
 */
class SystemTaskSimulation extends Simulation {

  private def propOrEnv(propKey: String, envCandidates: Seq[String]): Option[String] = {
    sys.props
      .get(propKey)
      .orElse(envCandidates.to(LazyList).flatMap(sys.env.get).headOption)
  }

  private val baseUrl =
    propOrEnv(
      "taskService.baseUrl",
      Seq(
        "SYSTEM_PERF_BASE_URL",
        "TASK_SERVICE_PERF_BASE_URL",
        "TASK_SERVICE_BASE_URL"
      )
    ).getOrElse("http://localhost:8080")

  private val loginPath =
    propOrEnv(
      "taskService.loginPath",
      Seq(
        "SYSTEM_PERF_LOGIN_PATH",
        "TASK_SERVICE_PERF_LOGIN_PATH",
        "TASK_SERVICE_LOGIN_PATH"
      )
    ).getOrElse("/system/auth/login")

  private val defaultUsername =
    propOrEnv(
      "taskService.username",
      Seq(
        "SYSTEM_PERF_USERNAME",
        "TASK_SERVICE_PERF_USERNAME",
        "TASK_SERVICE_USERNAME"
      )
    ).getOrElse("lushuwen1")

  private val defaultPassword =
    propOrEnv(
      "taskService.password",
      Seq(
        "SYSTEM_PERF_PASSWORD",
        "TASK_SERVICE_PERF_PASSWORD",
        "TASK_SERVICE_PASSWORD"
      )
    ).getOrElse("lushuwen1")

  private val defaultEventId =
    propOrEnv(
      "taskService.eventId",
      Seq(
        "SYSTEM_PERF_EVENT_ID",
        "TASK_SERVICE_PERF_EVENT_ID",
        "TASK_SERVICE_EVENT_ID"
      )
    ).getOrElse("1")

  private val defaultTaskId =
    propOrEnv(
      "taskService.taskId",
      Seq(
        "SYSTEM_PERF_TASK_ID",
        "TASK_SERVICE_PERF_TASK_ID",
        "TASK_SERVICE_TASK_ID"
      )
    ).getOrElse("1")

  private val credentialFeeder =
    Iterator.continually(
      Map(
        "username" -> defaultUsername,
        "password" -> defaultPassword,
        "eventId" -> defaultEventId,
        "taskId" -> defaultTaskId
      ))

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  private val loginAction =
    exec(
      http("login")
        .post(loginPath)
        .body(
          StringBody(
            """{"username":"${username}","password":"${password}"}"""
          ))
        .asJson
        .check(status.is(200))
    ).exitHereIfFailed

  private val getTaskScenario =
    scenario("Login and Get Task")
      .feed(credentialFeeder)
      .exec(loginAction)
      .pause(500.millis)
      .exec(
        http("get-task")
          .get("/system/task/${eventId}/${taskId}")
          .check(status.in(200, 404))
      )

  setUp(
    getTaskScenario.inject(
      rampUsers(20).during(30.seconds),
      constantUsersPerSec(20).during(60.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile3.lte(70000),
      global.successfulRequests.percent.gte(80),
      forAll.failedRequests.percent.lte(5)
    )
}
