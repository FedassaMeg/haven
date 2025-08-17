package org.haven.clientprofile.application.services;

import org.haven.clientprofile.application.commands.*;
import org.haven.clientprofile.application.queries.*;
import org.haven.clientprofile.application.dto.*;
import org.haven.clientprofile.domain.*;
import org.haven.shared.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClientAppService {
    
    private final ClientRepository clientRepository;
    private final ClientDomainService clientDomainService;
    
    public ClientAppService(ClientRepository clientRepository, ClientDomainService clientDomainService) {
        this.clientRepository = clientRepository;
        this.clientDomainService = clientDomainService;
    }
    
    public ClientId handle(CreateClientCmd cmd) {
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL,
            cmd.familyName(),
            List.of(cmd.givenName()),
            List.of(),
            List.of(),
            null
        );
        
        clientDomainService.validateClientCreation(name, cmd.gender());
        
        Client client = Client.create(name, cmd.gender(), cmd.birthDate());
        clientRepository.save(client);
        return client.getId();
    }
    
    public void handle(UpdateDemographicsCmd cmd) {
        Client client = clientRepository.findById(cmd.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + cmd.clientId()));
        
        var name = new HumanName(
            HumanName.NameUse.OFFICIAL,
            cmd.familyName(),
            List.of(cmd.givenName()),
            List.of(),
            List.of(),
            null
        );
        
        client.updateDemographics(name, cmd.gender(), cmd.birthDate());
        clientRepository.save(client);
    }
    
    public void handle(AddClientAddressCmd cmd) {
        Client client = clientRepository.findById(cmd.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + cmd.clientId()));
            
        client.addAddress(cmd.address());
        clientRepository.save(client);
    }
    
    public void handle(AddClientTelecomCmd cmd) {
        Client client = clientRepository.findById(cmd.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + cmd.clientId()));
            
        client.addTelecom(cmd.telecom());
        clientRepository.save(client);
    }
    
    public void handle(AddHouseholdMemberCmd cmd) {
        Client client = clientRepository.findById(cmd.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + cmd.clientId()));
            
        var member = new HouseholdMember(cmd.memberId(), cmd.relationship());
        
        if (!clientDomainService.canAddHouseholdMember(client, member)) {
            throw new IllegalStateException("Cannot add household member");
        }
        
        client.addHouseholdMember(member);
        clientRepository.save(client);
    }
    
    @Transactional(readOnly = true)
    public Optional<ClientDto> handle(GetClientQuery query) {
        return clientRepository.findById(query.clientId())
            .map(this::toDto);
    }
    
    @Transactional(readOnly = true)
    public List<ClientDto> handle(SearchClientsQuery query) {
        if (query.name() != null) {
            return clientRepository.findByNameContaining(query.name())
                .stream().map(this::toDto).toList();
        }
        
        if (query.activeOnly()) {
            return clientRepository.findActiveClients()
                .stream().map(this::toDto).toList();
        }
        
        return List.of(); // Would implement pagination in real world
    }
    
    private ClientDto toDto(Client client) {
        return new ClientDto(
            client.getId().value(),
            client.getPrimaryName(),
            client.getGender(),
            client.getBirthDate(),
            client.getAddresses(),
            client.getTelecoms(),
            client.getStatus(),
            client.getCreatedAt()
        );
    }
}
