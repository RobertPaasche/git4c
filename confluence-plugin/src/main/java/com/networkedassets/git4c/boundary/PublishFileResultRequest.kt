package com.networkedassets.git4c.boundary

import com.networkedassets.git4c.core.usecase.async.BackendRequestForAsyncResult


class PublishFileResultRequest(requestId: String) : BackendRequestForAsyncResult<Unit>(requestId)