package usonia.server.auth

import usonia.server.http.HttpRequest
import usonia.server.http.SocketCall

interface Authorization
{
    suspend fun validate(request: HttpRequest): AuthResult
    suspend fun validate(call: SocketCall): AuthResult
}
