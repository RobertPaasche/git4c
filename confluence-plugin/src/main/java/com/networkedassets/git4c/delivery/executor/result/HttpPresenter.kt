package com.networkedassets.git4c.delivery.executor.result

import com.atlassian.confluence.core.service.NotAuthorizedException
import com.github.kittinunf.result.Result
import com.networkedassets.git4c.boundary.outbound.exceptions.ConflictException
import com.networkedassets.git4c.boundary.outbound.exceptions.NotFoundException
import com.networkedassets.git4c.boundary.outbound.exceptions.NotReadyException
import com.networkedassets.git4c.delivery.executor.monitoring.BackendTimer
import com.networkedassets.git4c.delivery.executor.monitoring.TransactionInfo
import com.networkedassets.git4c.utils.SerializationUtils.serialize
import java.util.*
import java.util.Objects.isNull
import java.util.concurrent.TimeoutException
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.*

class HttpPresenter() : BackendPresenter<Response, Response> {

    private var stopwatch: Optional<BackendTimer.Stopwatch> = Optional.empty<BackendTimer.Stopwatch>()

    override fun render(result: Any): Response {
        if (result is Result<*, Exception>) {
            return render(result)
        } else {
            return error(IllegalArgumentException())
        }
    }

    override fun error(exception: Throwable): Response {
        val response = getErrorFromException(exception)
        stopwatch.ifPresent { e -> e.stopAndLog(exception, response.status) }
        return response
    }

    private fun getErrorFromException(exception: Throwable) = when (exception) {
        is IllegalArgumentException -> status(400)
        is NotAuthorizedException -> status(401)
        is NotFoundException -> status(404)
        is TimeoutException -> status(408)
        is ConflictException -> status(409)
        is NotReadyException -> status(202)
        else -> status(500)
    }.entity(exception.message).build()

    protected fun render(result: Result<*, Exception>): Response {
        if (isNull(result.component2())) {
            val response = contentToResponse(result.component1())
            stopwatch.ifPresent { e -> e.stopAndLog(response.status) }
            return response
        }
        return error(result.component2()!!)
    }

    private fun contentToResponse(content: Any?): Response =
            if (content == null) status(Status.NOT_FOUND).build()
            else ok().entity(serialize(content)).build()

    fun startStopwatch(timer: BackendTimer, transactionInfo: TransactionInfo): BackendPresenter<Response, Response> {
        this.stopwatch = Optional.of(timer.start(transactionInfo))
        return this
    }
}
