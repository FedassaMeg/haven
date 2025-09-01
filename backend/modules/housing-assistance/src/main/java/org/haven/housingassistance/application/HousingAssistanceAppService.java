package org.haven.housingassistance.application;

import org.haven.housingassistance.application.commands.AuthorizePaymentCmd;
import org.haven.housingassistance.application.commands.SendLandlordCommunicationCmd;
import org.haven.housingassistance.application.dto.LandlordCommunicationDto;
import org.haven.housingassistance.application.dto.PaymentDto;
import org.haven.housingassistance.application.queries.GetLandlordCommunicationsQuery;
import org.haven.housingassistance.application.queries.GetPaymentsByEnrollmentQuery;
import org.haven.housingassistance.application.services.LandlordCommunicationService;
import org.haven.housingassistance.domain.HousingAssistance;
import org.haven.housingassistance.domain.HousingAssistanceId;
import org.haven.housingassistance.domain.HousingAssistanceRepository;
import org.haven.housingassistance.domain.LandlordCommunication;
import org.haven.housingassistance.domain.LandlordCommunicationRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HousingAssistanceAppService {

    private final HousingAssistanceRepository housingAssistanceRepository;
    private final LandlordCommunicationRepository landlordCommunicationRepository;
    private final LandlordCommunicationService landlordCommunicationService;

    public HousingAssistanceAppService(
            HousingAssistanceRepository housingAssistanceRepository,
            LandlordCommunicationRepository landlordCommunicationRepository,
            LandlordCommunicationService landlordCommunicationService) {
        this.housingAssistanceRepository = housingAssistanceRepository;
        this.landlordCommunicationRepository = landlordCommunicationRepository;
        this.landlordCommunicationService = landlordCommunicationService;
    }

    public void authorizePayment(AuthorizePaymentCmd command) {
        HousingAssistance assistance = housingAssistanceRepository
            .findById(new HousingAssistanceId(command.housingAssistanceId()))
            .orElseThrow(() -> new IllegalArgumentException("Housing assistance not found"));

        assistance.authorizePayment(
            command.amount(),
            command.paymentDate(),
            command.paymentType(),
            command.subtype(),
            command.periodStart(),
            command.periodEnd(),
            command.payeeId(),
            command.payeeName(),
            command.authorizedBy()
        );
        housingAssistanceRepository.save(assistance);
    }

    public LandlordCommunicationDto sendLandlordCommunication(SendLandlordCommunicationCmd command) {
        // Create recipient contact from command
        var recipientContact = new LandlordCommunicationService.RecipientContact();
        recipientContact.setEmail(command.recipientEmail());
        recipientContact.setPhone(command.recipientPhone());
        recipientContact.setFax(command.recipientFax());
        recipientContact.setPortalId(command.recipientPortalId());
        
        LandlordCommunication communication = landlordCommunicationService.sendCommunication(
            command.clientId(),
            command.landlordId(),
            command.channel(),
            command.subject(),
            command.body(),
            command.requestedFields(),
            recipientContact,
            command.userId()
        );

        return LandlordCommunicationDto.from(communication);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByEnrollment(GetPaymentsByEnrollmentQuery query) {
        List<HousingAssistance> assistances = housingAssistanceRepository
            .findByEnrollmentId(new ProgramEnrollmentId(query.enrollmentId()));

        return assistances.stream()
            .flatMap(assistance -> assistance.getPayments().stream()
                .map(payment -> PaymentDto.from(
                    payment,
                    assistance.getId().getValue(),
                    assistance.getClientId().getValue(),
                    assistance.getEnrollmentId().getValue()
                )))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LandlordCommunicationDto> getLandlordCommunications(GetLandlordCommunicationsQuery query) {
        List<LandlordCommunication> communications = landlordCommunicationRepository
            .findByClientIdAndLandlordId(query.clientId(), query.landlordId());

        return communications.stream()
            .map(LandlordCommunicationDto::from)
            .toList();
    }
}