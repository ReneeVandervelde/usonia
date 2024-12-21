package usonia.server.auth

import usonia.server.http.HttpRequest

interface Authorization
{
    suspend fun validate(request: HttpRequest): AuthResult
}
