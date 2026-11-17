package org.ouraircraft.model.dto.convertors

import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.dto.data.AircraftDto
import org.ouraircraft.model.dto.data.AircraftTypeDto
import org.ouraircraft.model.dto.data.ManufacturerDto

fun Manufacturer.toDto(): ManufacturerDto = ManufacturerDto(
    id,
    name,
    longName
)

fun Aircraft.toDto(): AircraftDto = AircraftDto(
    id,
    type.toDto(),
    registration.toString() // maybe later format this
)

fun AircraftType.toDto(): AircraftTypeDto = AircraftTypeDto(
    id = id,
    manufacturer = manufacturer.toDto(),
    family = family,
    variant = variant,
    icaoDesignator = icaoDesignator,
    engineCount = engineCount,
    engineType = engineType,
    wakeCategory = wakeCategory
)
