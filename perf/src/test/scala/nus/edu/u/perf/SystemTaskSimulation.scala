package nus.edu.u.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Gatling simulation for ChronoFlow task APIs.
 *
 * Environment variables / system properties:
 * - base URL: `taskService.baseUrl` / `SYSTEM_PERF_BASE_URL` / `TASK_SERVICE_BASE_URL`
 * - login path: `taskService.loginPath` / `SYSTEM_PERF_LOGIN_PATH`
 * - username/password: `taskService.username` / `SYSTEM_PERF_USERNAME`, `taskService.password` / `SYSTEM_PERF_PASSWORD`
 * - IDs: `taskService.eventId` / `SYSTEM_PERF_EVENT_ID`, `taskService.taskId` / `SYSTEM_PERF_TASK_ID`
 */
class SystemTaskSimulation extends Simulation {

  private def resolve(key: String, envKeys: Seq[String]): Option[String] =
    sys.props.get(key).orElse(envKeys.view.flatMap(sys.env.get).headOption)

  private val baseUrl = resolve("taskService.baseUrl", Seq("SYSTEM_PERF_BASE_URL", "TASK_SERVICE_BASE_URL"))
    .getOrElse("http://localhost:8080")

  private val loginPath = resolve("taskService.loginPath", Seq("SYSTEM_PERF_LOGIN_PATH"))
    .getOrElse("/system/auth/login")

  private val defaultUsername = resolve("taskService.username", Seq("SYSTEM_PERF_USERNAME"))
    .getOrElse("admin")

  private val defaultPassword = resolve("taskService.password", Seq("SYSTEM_PERF_PASSWORD"))
    .getOrElse("admin")

  private val defaultEventId = resolve("taskService.eventId", Seq("SYSTEM_PERF_EVENT_ID"))
    .getOrElse("1")

  private val defaultTaskId = resolve("taskService.taskId", Seq("SYSTEM_PERF_TASK_ID"))
    .getOrElse("1")

  private val credentials =
    Iterator.continually(Map(
      "username" -> defaultUsername,
      "password" -> defaultPassword,
      "eventId" -> defaultEventId,
      "taskId" -> defaultTaskId
    ))

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  private val login =
    exec(
      http("login")
        .post(loginPath)
        .body(StringBody("""{"username":"${username}","password":"${password}"}"""))
        .asJson
        .check(status.is(200))
    ).exitHereIfFailed

  private val scenarioBuilder =
    scenario("Login and Get Task")
      .feed(credentials)
      .exec(login)
      .pause(500.millis)
      .exec(
        http("get-task")
          .get("/system/task/${eventId}/${taskId}")
          .check(status.in(200, 404))
      )

  setUp(
    scenarioBuilder.inject(
      rampUsers(100).during(30.seconds),
      constantUsersPerSec(100).during(60.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile3.lte(70000),
      global.successfulRequests.percent.gte(80),
      forAll.failedRequests.percent.lte(5)
    )
}
