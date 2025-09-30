package org.haven.financialassistance.infrastructure.services;

import org.haven.financialassistance.application.services.FinancialAlertsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of NotificationService for financial alerts.
 * Currently logs alerts but can be extended to send emails, SMS, etc.
 */
@Service
public class DefaultNotificationService implements FinancialAlertsService.NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultNotificationService.class);

    @Override
    public void sendFinancialAlert(String recipient, FinancialAlertsService.FinancialAlert alert) {
        logger.warn("Financial Alert [{}] for {}: {} - {} (Amount: ${})",
            alert.severity(),
            recipient,
            alert.title(),
            alert.message(),
            alert.amount()
        );

        // TODO: Implement actual notification mechanisms (email, SMS, Slack, etc.)
        // For now, we just log the alert
        logger.info("Alert details - Type: {}, Client ID: {}, Ledger ID: {}, Date: {}",
            alert.type(),
            alert.clientId(),
            alert.ledgerId(),
            alert.alertDate()
        );
    }
}