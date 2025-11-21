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

@Service
public class ReportService {

    private final ProductRepository productRepository;
    private final BidRepository bidRepository;

    public ReportService(ProductRepository productRepository, BidRepository bidRepository) {
        this.productRepository = productRepository;
        this.bidRepository = bidRepository;
    }

    public byte[] generateProductReport() throws IOException {
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
                } else {
                    row.createCell(2).setCellValue(0);
                    row.createCell(3).setCellValue(0);
                    row.createCell(4).setCellValue(0);
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
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
