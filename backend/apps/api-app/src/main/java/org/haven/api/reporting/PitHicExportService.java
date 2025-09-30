package org.haven.api.reporting;

import org.haven.reporting.application.services.PitHicAggregationService;
import org.haven.reporting.domain.pithic.HicInventoryData;
import org.haven.reporting.domain.pithic.PitCensusData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PitHicExportService {

    private final PitHicAggregationService aggregationService;

    public PitHicExportService(PitHicAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public PitCensusData getPitCensusData(UUID censusId) {
        return aggregationService.findPitCensus(censusId)
            .orElseThrow(() -> new IllegalArgumentException("PIT census not found: " + censusId));
    }

    public HicInventoryData getHicInventoryData(UUID inventoryId) {
        return aggregationService.findHicInventory(inventoryId)
            .orElseThrow(() -> new IllegalArgumentException("HIC inventory not found: " + inventoryId));
    }

    public List<ReportSummary> getAvailableReports(LocalDate startDate,
                                                   LocalDate endDate,
                                                   String continuumCode,
                                                   String organizationId) {
        List<ReportSummary> summaries = new ArrayList<>();
        for (PitCensusData pit : aggregationService.listPitCensusBetween(startDate, endDate)) {
            summaries.add(ReportSummary.forPit(pit));
        }
        for (HicInventoryData hic : aggregationService.listHicInventoryBetween(startDate, endDate)) {
            summaries.add(ReportSummary.forHic(hic));
        }
        return summaries;
    }

    public AnnualPitHicReport getAnnualReport(int year, String continuumCode, String organizationId) {
        List<PitCensusData> pit = aggregationService.listPitCensusBetween(
            LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        List<HicInventoryData> hic = aggregationService.listHicInventoryBetween(
            LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return new AnnualPitHicReport(year, continuumCode, organizationId, pit, hic);
    }

    public void exportPitCensusCsv(PitCensusData data, OutputStream outputStream) throws IOException {
        writePlaceholderCsv("PIT Census", data.getCensusId(), data.getCensusDate(), outputStream);
    }

    public void exportPitCensusPdf(PitCensusData data, OutputStream outputStream) throws IOException {
        writePlaceholderBinary("PIT Census PDF", outputStream);
    }

    public void exportPitCensusJson(PitCensusData data, OutputStream outputStream) throws IOException {
        writePlaceholderJson("PIT Census", data.getCensusId(), outputStream);
    }

    public void exportPitCensusXml(PitCensusData data, OutputStream outputStream) throws IOException {
        writePlaceholderXml("PIT Census", data.getCensusId(), outputStream);
    }

    public void exportHicInventoryCsv(HicInventoryData data, OutputStream outputStream) throws IOException {
        writePlaceholderCsv("HIC Inventory", data.getInventoryId(), data.getInventoryDate(), outputStream);
    }

    public void exportHicInventoryPdf(HicInventoryData data, OutputStream outputStream) throws IOException {
        writePlaceholderBinary("HIC Inventory PDF", outputStream);
    }

    public void exportHicInventoryJson(HicInventoryData data, OutputStream outputStream) throws IOException {
        writePlaceholderJson("HIC Inventory", data.getInventoryId(), outputStream);
    }

    public void exportHicInventoryXml(HicInventoryData data, OutputStream outputStream) throws IOException {
        writePlaceholderXml("HIC Inventory", data.getInventoryId(), outputStream);
    }

    private void writePlaceholderCsv(String label, UUID id, LocalDate date, OutputStream out) throws IOException {
        String csv = String.format("type,id,date\n%s,%s,%s\n", label, id, date);
        out.write(csv.getBytes(StandardCharsets.UTF_8));
    }

    private void writePlaceholderBinary(String label, OutputStream out) throws IOException {
        out.write((label + " placeholder\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writePlaceholderJson(String label, UUID id, OutputStream out) throws IOException {
        String json = String.format("{\"type\":\"%s\",\"id\":\"%s\"}\n", label, id);
        out.write(json.getBytes(StandardCharsets.UTF_8));
    }

    private void writePlaceholderXml(String label, UUID id, OutputStream out) throws IOException {
        String xml = String.format("<report><type>%s</type><id>%s</id></report>\n", label, id);
        out.write(xml.getBytes(StandardCharsets.UTF_8));
    }

    public record ReportSummary(UUID reportId,
                                String reportType,
                                LocalDate reportDate,
                                String continuumCode,
                                String organizationId) {

        public static ReportSummary forPit(PitCensusData data) {
            return new ReportSummary(
                data.getCensusId(),
                "PIT_CENSUS",
                data.getCensusDate(),
                data.getContinuumCode(),
                data.getOrganizationId()
            );
        }

        public static ReportSummary forHic(HicInventoryData data) {
            return new ReportSummary(
                data.getInventoryId(),
                "HIC_INVENTORY",
                data.getInventoryDate(),
                data.getContinuumCode(),
                data.getOrganizationId()
            );
        }
    }
}
