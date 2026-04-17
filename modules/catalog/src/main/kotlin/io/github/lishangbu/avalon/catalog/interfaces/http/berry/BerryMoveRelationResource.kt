package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.application.berry.BerryDetailApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/catalog/berries/{id}/moves")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BerryMoveRelationResource(
    private val service: BerryDetailApplicationService,
) {
    @GET
    suspend fun list(@PathParam("id") id: UUID): List<BerryMoveRelationResponse> =
        service.listMoveRelations(id).map { it.toResponse() }

    @POST
    suspend fun create(
        @PathParam("id") id: UUID,
        @Valid request: UpsertBerryMoveRelationRequest,
    ): BerryMoveRelationResponse = service.createMoveRelation(id, request.toDraft()).toResponse()

    @DELETE
    @Path("/{relationId}")
    suspend fun delete(
        @PathParam("id") id: UUID,
        @PathParam("relationId") relationId: UUID,
    ): Response {
        service.deleteMoveRelation(id, relationId)
        return Response.noContent().build()
    }
}
