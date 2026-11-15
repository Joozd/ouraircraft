package org.ouraircraft.model.dto.convertors

import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.ManufacturerProposal
import org.ouraircraft.model.data.valueclasses.AircraftRegistration
import org.ouraircraft.model.dto.data.DeleteAircraftProposalDto
import org.ouraircraft.model.dto.data.DeleteAircraftTypeProposalDto
import org.ouraircraft.model.dto.data.DeleteManufacturerProposalDto
import org.ouraircraft.model.dto.data.NewAircraftProposalDto
import org.ouraircraft.model.dto.data.NewAircraftTypeProposalDto
import org.ouraircraft.model.dto.data.NewManufacturerProposalDto

/**
 * Converts a manufacturer creation or update DTO into a proposal.
 *
 * @param user The user submitting the proposal.
 * @param targetId The manufacturer to update, or null to create a new manufacturer.
 */
fun NewManufacturerProposalDto.toManufacturerProposal(
    user: User,
    targetId: Long? = null,
): ManufacturerProposal =
    ManufacturerProposal(
        user = user,
        targetId = targetId,
        delete = false,
        proposalComment = proposalComment,
        name = name,
        longName = longName,
    )

/**
 * Converts a manufacturer deletion DTO into a proposal.
 *
 * @param user The user submitting the proposal.
 * @param targetId The manufacturer to delete.
 */
fun DeleteManufacturerProposalDto.toManufacturerProposal(
    user: User,
    targetId: Long,
): ManufacturerProposal =
    ManufacturerProposal(
        user = user,
        targetId = targetId,
        delete = true,
        proposalComment = proposalComment,
        name = "", // Ignored for deletion.
        longName = null, // Ignored for deletion.
    )

/**
 * Converts an aircraft type creation or update DTO into a proposal.
 *
 * @param user The user submitting the proposal.
 * @param targetId The aircraft type to update, or null to create a new aircraft type.
 */
fun NewAircraftTypeProposalDto.toAircraftTypeProposal(
    user: User,
    targetId: Long? = null,
): AircraftTypeProposal =
    AircraftTypeProposal(
        user = user,
        targetId = targetId,
        delete = false,
        proposalComment = proposalComment,
        manufacturerId = manufacturerId,
        family = family,
        variant = variant,
        icaoDesignator = icaoDesignator,
        engineCount = engineCount,
        engineType = engineType,
        wakeCategory = wakeCategory,
    )

/**
 * Converts an aircraft type deletion DTO into a proposal.
 *
 * @param user The user submitting the proposal.
 * @param targetId The aircraft type to delete.
 */
fun DeleteAircraftTypeProposalDto.toAircraftTypeProposal(
    user: User,
    targetId: Long,
): AircraftTypeProposal =
    AircraftTypeProposal(
        user = user,
        targetId = targetId,
        delete = true,
        proposalComment = proposalComment,
        manufacturerId = 0L, // Ignored for deletion.
        family = "", // Ignored for deletion.
        variant = "", // Ignored for deletion.
        icaoDesignator = "", // Ignored for deletion.
        engineCount = 0, // Ignored for deletion.
        engineType = "", // Ignored for deletion.
        wakeCategory = "", // Ignored for deletion.
    )

/**
 * Converts an aircraft creation or update DTO into a proposal.
 *
 * Normalizes the registration to uppercase letters and digits.
 *
 * @param user The user submitting the proposal.
 * @param targetId The aircraft to update, or null to create a new aircraft.
 * @throws IllegalArgumentException If the normalized registration is empty.
 */
fun NewAircraftProposalDto.toAircraftProposal(
    user: User,
    targetId: Long? = null,
): AircraftProposal =
    AircraftProposal(
        user = user,
        targetId = targetId,
        delete = false,
        proposalComment = proposalComment,
        aircraftTypeId = aircraftTypeId,
        registration = AircraftRegistration.normalizeFromString(registration),
    )

/**
 * Converts an aircraft deletion DTO into a proposal.
 *
 * @param user The user submitting the proposal.
 * @param targetId The aircraft to delete.
 */
fun DeleteAircraftProposalDto.toAircraftProposal(
    user: User,
    targetId: Long,
): AircraftProposal =
    AircraftProposal(
        user = user,
        targetId = targetId,
        delete = true,
        proposalComment = proposalComment,
        aircraftTypeId = 0L, // Ignored for deletion.
        registration = AircraftRegistration("IGNORED"), // Valid placeholder; ignored for deletion.
    )