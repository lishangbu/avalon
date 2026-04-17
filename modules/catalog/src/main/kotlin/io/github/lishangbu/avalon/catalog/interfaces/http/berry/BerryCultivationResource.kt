package io.github.lishangbu.avalon.catalog.interfaces.http.berry

import io.github.lishangbu.avalon.catalog.application.berry.BerryDetailApplicationService
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import java.util.UUID

@Path("/catalog/berries/{id}/cultivation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BerryCultivationResource(
    private val service: BerryDetailApplicationService,
) {
    @GET
    suspend fun get(@PathParam("id") id: UUID): BerryCultivationProfileResponse =
        service.getCultivationProfile(id).toResponse()

    @PUT
    suspend fun put(
        @PathParam("id") id: UUID,
        @Valid request: UpsertBerryCultivationProfileRequest,
    ): BerryCultivationProfileResponse = service.saveCultivationProfile(id, request.toDraft()).toResponse()
}
