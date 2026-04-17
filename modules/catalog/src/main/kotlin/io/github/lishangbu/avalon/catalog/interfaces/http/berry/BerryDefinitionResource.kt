package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.application.berry.BerryDefinitionApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/catalog/berries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BerryDefinitionResource(
    private val service: BerryDefinitionApplicationService,
) {
    @GET
    suspend fun list(): List<BerryDefinitionResponse> = service.listBerryDefinitions().map { it.toResponse() }

    @GET
    @Path("/{id}")
    suspend fun getById(@PathParam("id") id: UUID): BerryDefinitionResponse = service.getBerryDefinition(id).toResponse()

    @POST
    suspend fun create(@Valid request: UpsertBerryDefinitionRequest): BerryDefinitionResponse =
        service.createBerryDefinition(request.toDraft()).toResponse()

    @PUT
    @Path("/{id}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @Valid request: UpsertBerryDefinitionRequest,
    ): BerryDefinitionResponse = service.updateBerryDefinition(id, request.toDraft()).toResponse()

    @DELETE
    @Path("/{id}")
    suspend fun delete(@PathParam("id") id: UUID): Response {
        service.deleteBerryDefinition(id)
        return Response.noContent().build()
    }
}
