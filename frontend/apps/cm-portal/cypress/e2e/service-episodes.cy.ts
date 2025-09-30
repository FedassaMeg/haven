describe('Service Episodes', () => {
  beforeEach(() => {
    // Login as case manager
    cy.login('case_manager', 'password');

    // Navigate to a client page
    cy.visit('/clients/test-client-id/services');

    // Wait for page to load
    cy.get('[data-testid="service-episodes-page"]').should('be.visible');
  });

  describe('Service Episode Creation', () => {
    it('should create a new service episode', () => {
      // Click create service episode button
      cy.get('[data-testid="create-service-button"]').click();

      // Fill out the service episode form
      cy.get('[data-testid="service-type-select"]').click();
      cy.get('[data-value="CASE_MANAGEMENT"]').click();

      cy.get('[data-testid="delivery-mode-select"]').click();
      cy.get('[data-value="IN_PERSON"]').click();

      cy.get('[data-testid="service-date-input"]').type('2024-05-15');
      cy.get('[data-testid="planned-duration-input"]').type('60');
      cy.get('[data-testid="provider-name-input"]').type('Test Provider');

      cy.get('[data-testid="funding-source-select"]').click();
      cy.get('[data-value="HUD-COC"]').click();

      cy.get('[data-testid="service-description-textarea"]').type('Test service description');

      // Submit the form
      cy.get('[data-testid="create-service-submit"]').click();

      // Verify service was created
      cy.get('[data-testid="success-message"]').should('contain', 'Service episode created successfully');
      cy.get('[data-testid="service-episode-list"]').should('contain', 'CASE_MANAGEMENT');
    });

    it('should validate required fields', () => {
      // Click create service episode button
      cy.get('[data-testid="create-service-button"]').click();

      // Try to submit without filling required fields
      cy.get('[data-testid="create-service-submit"]').click();

      // Verify validation errors
      cy.get('[data-testid="service-type-error"]').should('contain', 'Service type is required');
      cy.get('[data-testid="delivery-mode-error"]').should('contain', 'Delivery mode is required');
      cy.get('[data-testid="provider-name-error"]').should('contain', 'Provider name is required');
    });

    it('should use quick service templates', () => {
      // Click create service episode button
      cy.get('[data-testid="create-service-button"]').click();

      // Click crisis intervention template
      cy.get('[data-testid="quick-template-crisis"]').click();

      // Verify form is pre-filled
      cy.get('[data-testid="service-type-select"]').should('contain', 'Crisis Intervention');
      cy.get('[data-testid="delivery-mode-select"]').should('contain', 'In Person');
      cy.get('[data-testid="planned-duration-input"]').should('have.value', '60');
      cy.get('[data-testid="confidential-checkbox"]').should('be.checked');
    });
  });

  describe('Service Episode Management', () => {
    beforeEach(() => {
      // Create a test service episode first
      cy.createTestServiceEpisode();
    });

    it('should start a service', () => {
      // Find scheduled service and start it
      cy.get('[data-testid="service-episode-item"]').first().within(() => {
        cy.get('[data-testid="start-service-button"]').click();
      });

      // Fill out start service form
      cy.get('[data-testid="start-location-input"]').type('Office Room A');
      cy.get('[data-testid="start-service-submit"]').click();

      // Verify service status changed
      cy.get('[data-testid="service-episode-item"]').first().should('contain', 'IN_PROGRESS');
    });

    it('should complete a service', () => {
      // Start a service first
      cy.startTestService();

      // Complete the service
      cy.get('[data-testid="service-episode-item"]').first().within(() => {
        cy.get('[data-testid="complete-service-button"]').click();
      });

      // Fill out completion form
      cy.get('[data-testid="service-outcome-textarea"]').type('Client made significant progress on housing goals');
      cy.get('[data-testid="service-notes-textarea"]').type('Excellent engagement throughout session');
      cy.get('[data-testid="complete-service-submit"]').click();

      // Verify service status changed
      cy.get('[data-testid="service-episode-item"]').first().should('contain', 'COMPLETED');
    });

    it('should update service outcome', () => {
      // Complete a service first
      cy.completeTestService();

      // Update outcome
      cy.get('[data-testid="service-episode-item"]').first().within(() => {
        cy.get('[data-testid="update-outcome-button"]').click();
      });

      // Update outcome form
      cy.get('[data-testid="outcome-textarea"]').clear().type('Updated outcome with follow-up requirements');
      cy.get('[data-testid="follow-up-required-textarea"]').type('Schedule follow-up appointment in 2 weeks');
      cy.get('[data-testid="follow-up-date-input"]').type('2024-05-29');
      cy.get('[data-testid="update-outcome-submit"]').click();

      // Verify outcome was updated
      cy.get('[data-testid="service-episode-item"]').first().should('contain', 'Updated outcome');
    });
  });

  describe('Service Episode Calendar', () => {
    it('should display services in calendar view', () => {
      // Switch to calendar view
      cy.get('[data-testid="calendar-tab"]').click();

      // Verify calendar is displayed
      cy.get('[data-testid="service-calendar"]').should('be.visible');

      // Navigate months
      cy.get('[data-testid="calendar-next-month"]').click();
      cy.get('[data-testid="calendar-previous-month"]').click();

      // Select today
      cy.get('[data-testid="calendar-today-button"]').click();
    });

    it('should show service details when clicking on calendar day', () => {
      // Switch to calendar view
      cy.get('[data-testid="calendar-tab"]').click();

      // Click on a day with services
      cy.get('[data-testid="calendar-day-with-services"]').first().click();

      // Verify day details panel is shown
      cy.get('[data-testid="day-details-panel"]').should('be.visible');
      cy.get('[data-testid="day-service-list"]').should('contain', 'service');
    });
  });

  describe('Service Episode Dashboard', () => {
    it('should display service statistics', () => {
      // Switch to dashboard view
      cy.get('[data-testid="dashboard-tab"]').click();

      // Verify statistics are displayed
      cy.get('[data-testid="total-services-metric"]').should('be.visible');
      cy.get('[data-testid="completed-services-metric"]').should('be.visible');
      cy.get('[data-testid="service-hours-metric"]').should('be.visible');
      cy.get('[data-testid="follow-up-needed-metric"]').should('be.visible');
    });

    it('should display service type breakdown', () => {
      // Switch to dashboard view
      cy.get('[data-testid="dashboard-tab"]').click();

      // Verify service type breakdown chart
      cy.get('[data-testid="service-type-breakdown"]').should('be.visible');
      cy.get('[data-testid="service-type-item"]').should('have.length.at.least', 1);
    });

    it('should display action items for overdue follow-ups', () => {
      // Create service with overdue follow-up
      cy.createServiceWithOverdueFollowUp();

      // Switch to dashboard view
      cy.get('[data-testid="dashboard-tab"]').click();

      // Verify action items section
      cy.get('[data-testid="action-items-section"]').should('be.visible');
      cy.get('[data-testid="overdue-follow-up-item"]').should('contain', 'Follow-up overdue');
    });
  });

  describe('Service Episode Reports', () => {
    it('should generate and display service reports', () => {
      // Switch to reports view
      cy.get('[data-testid="reports-tab"]').click();

      // Set report period
      cy.get('[data-testid="report-period-select"]').click();
      cy.get('[data-value="this_month"]').click();

      // Verify report sections
      cy.get('[data-testid="report-summary"]').should('be.visible');
      cy.get('[data-testid="service-type-analysis"]').should('be.visible');
      cy.get('[data-testid="funding-source-analysis"]').should('be.visible');
      cy.get('[data-testid="provider-performance"]').should('be.visible');
    });

    it('should export report data', () => {
      // Switch to reports view
      cy.get('[data-testid="reports-tab"]').click();

      // Export report
      cy.get('[data-testid="export-report-button"]').click();

      // Verify download initiated
      cy.readFile('cypress/downloads').should('exist');
    });

    it('should print report', () => {
      // Switch to reports view
      cy.get('[data-testid="reports-tab"]').click();

      // Trigger print
      cy.window().then((win) => {
        cy.stub(win, 'print').as('windowPrint');
      });

      cy.get('[data-testid="print-report-button"]').click();
      cy.get('@windowPrint').should('have.been.called');
    });
  });

  describe('Offline Functionality', () => {
    it('should work offline', () => {
      // Simulate going offline
      cy.window().then((win) => {
        win.navigator.onLine = false;
        win.dispatchEvent(new Event('offline'));
      });

      // Verify offline indicator
      cy.get('[data-testid="offline-indicator"]').should('be.visible');

      // Create service offline
      cy.get('[data-testid="create-service-button"]').click();
      cy.fillServiceForm();
      cy.get('[data-testid="create-service-submit"]').click();

      // Verify offline service saved
      cy.get('[data-testid="offline-service-saved"]').should('be.visible');
    });

    it('should sync when coming back online', () => {
      // Create offline service
      cy.createOfflineService();

      // Simulate coming back online
      cy.window().then((win) => {
        win.navigator.onLine = true;
        win.dispatchEvent(new Event('online'));
      });

      // Verify sync indicator
      cy.get('[data-testid="sync-indicator"]').should('be.visible');

      // Wait for sync to complete
      cy.get('[data-testid="sync-complete"]').should('be.visible');
    });
  });
});

// Custom commands for test helpers
Cypress.Commands.add('createTestServiceEpisode', () => {
  cy.request('POST', '/api/v1/service-episodes', {
    clientId: 'test-client-id',
    enrollmentId: 'test-enrollment-id',
    programId: 'TEST-PROG',
    programName: 'Test Program',
    serviceType: 'CASE_MANAGEMENT',
    deliveryMode: 'IN_PERSON',
    serviceDate: '2024-05-15',
    plannedDurationMinutes: 60,
    primaryProviderName: 'Test Provider',
    fundingSource: { funderId: 'HUD-COC', funderName: 'HUD Continuum of Care' },
    serviceDescription: 'Test service',
    isConfidential: false
  });
});

Cypress.Commands.add('startTestService', () => {
  cy.get('[data-testid="service-episode-item"]').first().within(() => {
    cy.get('[data-testid="start-service-button"]').click();
  });
  cy.get('[data-testid="start-location-input"]').type('Test Location');
  cy.get('[data-testid="start-service-submit"]').click();
});

Cypress.Commands.add('completeTestService', () => {
  cy.startTestService();
  cy.get('[data-testid="service-episode-item"]').first().within(() => {
    cy.get('[data-testid="complete-service-button"]').click();
  });
  cy.get('[data-testid="service-outcome-textarea"]').type('Test outcome');
  cy.get('[data-testid="complete-service-submit"]').click();
});

Cypress.Commands.add('fillServiceForm', () => {
  cy.get('[data-testid="service-type-select"]').click();
  cy.get('[data-value="CASE_MANAGEMENT"]').click();
  cy.get('[data-testid="delivery-mode-select"]').click();
  cy.get('[data-value="IN_PERSON"]').click();
  cy.get('[data-testid="planned-duration-input"]').type('60');
  cy.get('[data-testid="provider-name-input"]').type('Test Provider');
  cy.get('[data-testid="service-description-textarea"]').type('Test service');
});