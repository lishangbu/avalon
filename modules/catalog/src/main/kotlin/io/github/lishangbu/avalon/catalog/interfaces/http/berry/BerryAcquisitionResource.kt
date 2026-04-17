package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.application.berry.BerryDetailApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/catalog/berries/{id}/acquisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BerryAcquisitionResource(
    private val service: BerryDetailApplicationService,
) {
    @GET
    suspend fun list(@PathParam("id") id: UUID): List<BerryAcquisitionResponse> =
        service.listAcquisitions(id).map { it.toResponse() }

    @POST
    suspend fun create(
        @PathParam("id") id: UUID,
        @Valid request: UpsertBerryAcquisitionRequest,
    ): BerryAcquisitionResponse = service.createAcquisition(id, request.toDraft()).toResponse()

    @PUT
    @Path("/{acquisitionId}")
    suspend fun update(
        @PathParam("id") id: UUID,
        @PathParam("acquisitionId") acquisitionId: UUID,
        @Valid request: UpsertBerryAcquisitionRequest,
    ): BerryAcquisitionResponse = service.updateAcquisition(id, acquisitionId, request.toDraft()).toResponse()

    @DELETE
    @Path("/{acquisitionId}")
    suspend fun delete(
        @PathParam("id") id: UUID,
        @PathParam("acquisitionId") acquisitionId: UUID,
    ): Response {
        service.deleteAcquisition(id, acquisitionId)
        return Response.noContent().build()
    }
}
