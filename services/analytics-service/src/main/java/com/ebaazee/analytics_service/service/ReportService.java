package com.ebaazee.analytics_service.service;

import com.ebaazee.analytics_service.model.Bid;
import com.ebaazee.analytics_service.model.Product;
import com.ebaazee.analytics_service.repository.BidRepository;
import com.ebaazee.analytics_service.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReportService {

    // added
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ProductRepository productRepository;
    private final BidRepository bidRepository;

    public ReportService(ProductRepository productRepository, BidRepository bidRepository) {
        this.productRepository = productRepository;
        this.bidRepository = bidRepository;
    }

    public byte[] generateProductReport() throws IOException {
        log.debug("Starting product report generation");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Product Report");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Product ID", "Product Name", "Highest Bid Price", "Lowest Bid Price", "Total Bidders"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            List<Product> products = productRepository.findAll();
            log.info("Total products found for report: {}", products.size());

            int rowIndex = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());

                List<Bid> bids = bidRepository.findByProductId(product.getId());
                if (bids != null && !bids.isEmpty()) {
                    double highest = bids.stream().mapToDouble(Bid::getAmount).max().orElse(0);
                    double lowest = bids.stream().mapToDouble(Bid::getAmount).min().orElse(0);
                    row.createCell(2).setCellValue(highest);
                    row.createCell(3).setCellValue(lowest);
                    row.createCell(4).setCellValue(bids.size());

                    log.debug("Product {} has {} bids, highest: {}, lowest: {}", 
                              product.getId(), bids.size(), highest, lowest);
                } else {
                    row.createCell(2).setCellValue(0);
                    row.createCell(3).setCellValue(0);
                    row.createCell(4).setCellValue(0);

                    log.debug("Product {} has no bids", product.getId());
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Product report generated successfully ({} bytes)", out.size());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate product report", e);
            throw e;
        }
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
