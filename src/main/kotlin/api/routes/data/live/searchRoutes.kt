package org.ouraircraft.api.routes.data.live

import io.ktor.server.routing.*
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

internal fun Route.searchRoutes(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
){
    searchManufacturers(userManagementService, dataRetrievalService)
    searchAircraftTypes(userManagementService, dataRetrievalService)
    searchAircraft(userManagementService, dataRetrievalService)

    getManufacturer(userManagementService, dataRetrievalService)
    getAircraftType(userManagementService, dataRetrievalService)
    getAircraft(userManagementService, dataRetrievalService)
}