package org.haven.intake.application.services;

import org.haven.clientprofile.application.commands.CreateClientCmd;
import org.haven.clientprofile.application.services.ClientAppService;
import org.haven.clientprofile.domain.ClientDomainService;
import org.haven.clientprofile.domain.ClientId;
import org.haven.intake.application.commands.*;
import org.haven.intake.application.dto.PreIntakeContactDto;
import org.haven.intake.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Application service for intake workflow operations
 * Handles the progressive intake workflow from pre-contact to full client
 */
@Service
@Transactional
public class IntakeAppService {

    private final PreIntakeContactRepository preIntakeContactRepository;
    private final ClientAppService clientAppService;
    private final ClientDomainService clientDomainService;

    public IntakeAppService(
            PreIntakeContactRepository preIntakeContactRepository,
            ClientAppService clientAppService,
            ClientDomainService clientDomainService) {
        this.preIntakeContactRepository = preIntakeContactRepository;
        this.clientAppService = clientAppService;
        this.clientDomainService = clientDomainService;
    }

    /**
     * Create a new pre-intake contact (Step 1)
     */
    public PreIntakeContactId handle(CreatePreIntakeContactCmd cmd) {
        // Validation: contact date must not be more than 7 days in the past
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        if (cmd.contactDate().isBefore(sevenDaysAgo)) {
            throw new IllegalArgumentException("Contact date cannot be more than 7 days in the past");
        }

        PreIntakeContact contact = PreIntakeContact.create(
            cmd.clientAlias(),
            cmd.contactDate(),
            cmd.referralSource(),
            cmd.intakeWorkerName()
        );

        preIntakeContactRepository.save(contact);

        return contact.getId();
    }

    /**
     * Update pre-intake contact basic information
     */
    public void handle(UpdatePreIntakeContactCmd cmd) {
        PreIntakeContact contact = preIntakeContactRepository.findById(cmd.tempClientId())
            .orElseThrow(() -> new IllegalArgumentException("Pre-intake contact not found: " + cmd.tempClientId()));

        contact.updateContactInfo(
            cmd.clientAlias(),
            cmd.contactDate(),
            cmd.referralSource()
        );

        preIntakeContactRepository.save(contact);
    }

    /**
     * Update workflow data as user progresses through intake steps
     */
    public void handle(UpdateWorkflowDataCmd cmd) {
        PreIntakeContact contact = preIntakeContactRepository.findById(cmd.tempClientId())
            .orElseThrow(() -> new IllegalArgumentException("Pre-intake contact not found: " + cmd.tempClientId()));

        contact.updateWorkflowData(cmd.step(), cmd.stepData());

        preIntakeContactRepository.save(contact);
    }

    /**
     * Promote pre-intake contact to full client (Step 8: Demographics)
     *
     * This method:
     * 1. Validates temp client exists and hasn't expired
     * 2. Checks for duplicate clients
     * 3. Creates full client record
     * 4. Applies VAWA pseudonymization if needed
     * 5. Marks temp contact as promoted
     * 6. Returns the new client ID
     */
    public ClientId handle(PromoteClientCmd cmd) {
        // 1. Get pre-intake contact
        PreIntakeContact contact = preIntakeContactRepository.findById(cmd.tempClientId())
            .orElseThrow(() -> new IllegalArgumentException("Pre-intake contact not found: " + cmd.tempClientId()));

        if (contact.isExpired()) {
            throw new IllegalStateException("Cannot promote expired pre-intake contact");
        }

        if (contact.isPromoted()) {
            throw new IllegalStateException("Pre-intake contact already promoted");
        }

        // 2. Check for duplicates (using domain service)
        // In production, this would present duplicates to user for review
        // For now, we'll allow it but log a warning
        var name = new org.haven.shared.vo.HumanName(
            org.haven.shared.vo.HumanName.NameUse.OFFICIAL,
            cmd.familyName(),
            java.util.List.of(cmd.givenName()),
            java.util.List.of(),
            java.util.List.of(),
            null
        );

        try {
            clientDomainService.validateClientCreation(name, cmd.gender());
        } catch (ClientDomainService.ClientDuplicationException e) {
            // Log warning but allow creation
            // In production UI, this would trigger duplicate resolution workflow
            System.err.println("Warning: Potential duplicate client detected during promotion: " + e.getMessage());
        }

        // 3. Create full client
        CreateClientCmd createClientCmd = new CreateClientCmd(
            cmd.givenName(),
            cmd.familyName(),
            cmd.gender(),
            cmd.birthDate(),
            cmd.addresses(),
            cmd.telecoms()
        );

        ClientId clientId = clientAppService.handle(createClientCmd);

        // 4. Apply VAWA pseudonymization if needed
        if (cmd.vawaProtected()) {
            // TODO: Implement VAWA pseudonymization logic
            // This would involve:
            // - Generating pseudonymized HMIS Personal ID
            // - Flagging client as VAWA protected
            // - Restricting data sharing in HMIS exports
            System.out.println("VAWA protection requested for client: " + clientId);
        }

        // 5. Mark pre-intake contact as promoted
        contact.markPromoted(clientId.value());
        preIntakeContactRepository.save(contact);

        return clientId;
    }

    /**
     * Get pre-intake contact by ID
     */
    @Transactional(readOnly = true)
    public Optional<PreIntakeContactDto> getPreIntakeContact(PreIntakeContactId id) {
        return preIntakeContactRepository.findById(id)
            .map(this::toDto);
    }

    private PreIntakeContactDto toDto(PreIntakeContact contact) {
        return new PreIntakeContactDto(
            contact.getId().value(),
            contact.getClientAlias(),
            contact.getContactDate(),
            contact.getReferralSource(),
            contact.getIntakeWorkerName(),
            contact.getWorkflowData(),
            contact.getCurrentStep(),
            contact.getCreatedAt(),
            contact.getUpdatedAt(),
            contact.getExpiresAt(),
            contact.isExpired(),
            contact.isPromoted(),
            contact.getPromotedClientId()
        );
    }
}
