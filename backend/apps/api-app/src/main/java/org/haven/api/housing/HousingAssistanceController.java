package org.haven.api.housing;

import org.haven.housingassistance.application.HousingAssistanceAppService;
import org.haven.housingassistance.application.commands.AuthorizePaymentCmd;
import org.haven.housingassistance.application.commands.SendLandlordCommunicationCmd;
import org.haven.housingassistance.application.dto.LandlordCommunicationDto;
import org.haven.housingassistance.application.dto.PaymentDto;
import org.haven.housingassistance.application.queries.GetLandlordCommunicationsQuery;
import org.haven.housingassistance.application.queries.GetPaymentsByEnrollmentQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/housing-assistance")
public class HousingAssistanceController {

    private final HousingAssistanceAppService housingAssistanceAppService;

    public HousingAssistanceController(HousingAssistanceAppService housingAssistanceAppService) {
        this.housingAssistanceAppService = housingAssistanceAppService;
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<Void> authorizePayment(
            @PathVariable UUID id,
            @Valid @RequestBody AuthorizePaymentCmd cmd) {
        if (!id.equals(cmd.housingAssistanceId())) {
            return ResponseEntity.badRequest().build();
        }
        housingAssistanceAppService.authorizePayment(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/landlord-communications")
    public ResponseEntity<LandlordCommunicationDto> sendLandlordCommunication(
            @Valid @RequestBody SendLandlordCommunicationCmd cmd) {
        var communication = housingAssistanceAppService.sendLandlordCommunication(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(communication);
    }

    @GetMapping("/enrollments/{enrollmentId}/payments")
    public ResponseEntity<List<PaymentDto>> getPaymentsByEnrollment(
            @PathVariable UUID enrollmentId) {
        var query = new GetPaymentsByEnrollmentQuery(enrollmentId);
        var payments = housingAssistanceAppService.getPaymentsByEnrollment(query);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/landlord-communications")
    public ResponseEntity<List<LandlordCommunicationDto>> getLandlordCommunications(
            @RequestParam UUID clientId,
            @RequestParam UUID landlordId) {
        var query = new GetLandlordCommunicationsQuery(clientId, landlordId);
        var communications = housingAssistanceAppService.getLandlordCommunications(query);
        return ResponseEntity.ok(communications);
    }
}