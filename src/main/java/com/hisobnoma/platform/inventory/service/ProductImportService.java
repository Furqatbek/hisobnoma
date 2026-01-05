package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.inventory.dto.ImportResultDto;
import com.hisobnoma.platform.inventory.dto.ProductImportDto;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for importing products from CSV/Excel files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final SkuGeneratorService skuGeneratorService;
    private final SecurityContextHelper securityContextHelper;

    private static final String[] TEMPLATE_HEADERS = {
            "SKU", "Barcode", "Name", "Description", "Short Description",
            "Category", "Brand", "UOM Code", "Cost Price", "Selling Price",
            "Min Selling Price", "Wholesale Price", "Track Inventory", "Allow Negative Stock",
            "Min Stock Level", "Reorder Point", "Reorder Quantity", "Max Stock Level",
            "Active", "Service", "Sellable", "Purchasable", "Track Batch", "Track Serial",
            "Weight", "Weight Unit", "Manufacturer", "Manufacturer Part Number", "Tax Code", "Tags"
    };

    /**
     * Import products from a CSV file.
     */
    @Transactional
    public ImportResultDto importFromCsv(MultipartFile file) {
        try {
            List<ProductImportDto> rows = parseCsv(file);
            return processImport(rows);
        } catch (IOException e) {
            log.error("Failed to parse CSV file: {}", e.getMessage());
            throw new BusinessException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    /**
     * Import products from an Excel file.
     */
    @Transactional
    public ImportResultDto importFromExcel(MultipartFile file) {
        try {
            List<ProductImportDto> rows = parseExcel(file);
            return processImport(rows);
        } catch (IOException e) {
            log.error("Failed to parse Excel file: {}", e.getMessage());
            throw new BusinessException("Failed to parse Excel file: " + e.getMessage());
        }
    }

    /**
     * Generate a CSV template for importing products.
     */
    public byte[] generateCsvTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", TEMPLATE_HEADERS)).append("\n");
        sb.append("PROD001,1234567890123,Sample Product,Description,Short desc,Electronics,Samsung,PCS,100.00,150.00,120.00,130.00,true,false,10,20,50,100,true,false,true,true,false,false,1.5,kg,Samsung,SMP-001,VAT20,electronics,sample\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate an Excel template for importing products.
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Create sample row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("PROD001");
            sampleRow.createCell(1).setCellValue("1234567890123");
            sampleRow.createCell(2).setCellValue("Sample Product");
            sampleRow.createCell(3).setCellValue("Product description");
            sampleRow.createCell(4).setCellValue("Short description");
            sampleRow.createCell(5).setCellValue("Electronics");
            sampleRow.createCell(6).setCellValue("Samsung");
            sampleRow.createCell(7).setCellValue("PCS");
            sampleRow.createCell(8).setCellValue(100.00);
            sampleRow.createCell(9).setCellValue(150.00);
            sampleRow.createCell(10).setCellValue(120.00);
            sampleRow.createCell(11).setCellValue(130.00);
            sampleRow.createCell(12).setCellValue("true");
            sampleRow.createCell(13).setCellValue("false");
            sampleRow.createCell(14).setCellValue(10);
            sampleRow.createCell(15).setCellValue(20);
            sampleRow.createCell(16).setCellValue(50);
            sampleRow.createCell(17).setCellValue(100);
            sampleRow.createCell(18).setCellValue("true");
            sampleRow.createCell(19).setCellValue("false");
            sampleRow.createCell(20).setCellValue("true");
            sampleRow.createCell(21).setCellValue("true");
            sampleRow.createCell(22).setCellValue("false");
            sampleRow.createCell(23).setCellValue("false");
            sampleRow.createCell(24).setCellValue(1.5);
            sampleRow.createCell(25).setCellValue("kg");
            sampleRow.createCell(26).setCellValue("Samsung");
            sampleRow.createCell(27).setCellValue("SMP-001");
            sampleRow.createCell(28).setCellValue("VAT20");
            sampleRow.createCell(29).setCellValue("electronics,sample");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private List<ProductImportDto> parseCsv(MultipartFile file) throws IOException {
        List<ProductImportDto> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int rowNum = 0;
            boolean headerProcessed = false;

            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (!headerProcessed) {
                    headerProcessed = true;
                    continue; // Skip header row
                }

                String[] values = parseCsvLine(line);
                if (values.length < 3) continue; // Skip invalid rows

                ProductImportDto dto = mapToDto(values, rowNum);
                rows.add(dto);
            }
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());

        return values.toArray(new String[0]);
    }

    private List<ProductImportDto> parseExcel(MultipartFile file) throws IOException {
        List<ProductImportDto> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Start from 1 to skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String[] values = new String[TEMPLATE_HEADERS.length];
                for (int j = 0; j < TEMPLATE_HEADERS.length && j <= row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    values[j] = getCellValueAsString(cell);
                }

                if (values[2] == null || values[2].isEmpty()) continue; // Skip rows without name

                ProductImportDto dto = mapToDto(values, i + 1);
                rows.add(dto);
            }
        }
        return rows;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }

    private ProductImportDto mapToDto(String[] values, int rowNum) {
        return ProductImportDto.builder()
                .rowNumber(rowNum)
                .sku(getValueOrNull(values, 0))
                .barcode(getValueOrNull(values, 1))
                .name(getValueOrNull(values, 2))
                .description(getValueOrNull(values, 3))
                .shortDescription(getValueOrNull(values, 4))
                .categoryName(getValueOrNull(values, 5))
                .brandName(getValueOrNull(values, 6))
                .baseUomCode(getValueOrNull(values, 7))
                .costPrice(parseBigDecimal(getValueOrNull(values, 8)))
                .sellingPrice(parseBigDecimal(getValueOrNull(values, 9)))
                .minSellingPrice(parseBigDecimal(getValueOrNull(values, 10)))
                .wholesalePrice(parseBigDecimal(getValueOrNull(values, 11)))
                .trackInventory(parseBoolean(getValueOrNull(values, 12), true))
                .allowNegativeStock(parseBoolean(getValueOrNull(values, 13), false))
                .minStockLevel(parseBigDecimal(getValueOrNull(values, 14)))
                .reorderPoint(parseBigDecimal(getValueOrNull(values, 15)))
                .reorderQuantity(parseBigDecimal(getValueOrNull(values, 16)))
                .maxStockLevel(parseBigDecimal(getValueOrNull(values, 17)))
                .active(parseBoolean(getValueOrNull(values, 18), true))
                .service(parseBoolean(getValueOrNull(values, 19), false))
                .sellable(parseBoolean(getValueOrNull(values, 20), true))
                .purchasable(parseBoolean(getValueOrNull(values, 21), true))
                .trackBatch(parseBoolean(getValueOrNull(values, 22), false))
                .trackSerial(parseBoolean(getValueOrNull(values, 23), false))
                .weight(parseBigDecimal(getValueOrNull(values, 24)))
                .weightUnit(getValueOrNull(values, 25))
                .manufacturer(getValueOrNull(values, 26))
                .manufacturerPartNumber(getValueOrNull(values, 27))
                .taxCode(getValueOrNull(values, 28))
                .tags(getValueOrNull(values, 29))
                .build();
    }

    private String getValueOrNull(String[] values, int index) {
        if (index >= values.length || values[index] == null) return null;
        String val = values[index].trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private ImportResultDto processImport(List<ProductImportDto> rows) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ImportResultDto result = ImportResultDto.builder()
                .totalRows(rows.size())
                .successCount(0)
                .errorCount(0)
                .skippedCount(0)
                .build();

        // Pre-load lookups
        Map<String, Category> categoryMap = loadCategories(tenantId);
        Map<String, Brand> brandMap = loadBrands(tenantId);
        Map<String, UnitOfMeasure> uomMap = loadUoms(tenantId);

        for (ProductImportDto row : rows) {
            try {
                validateRow(row, tenantId, categoryMap, brandMap, uomMap);

                if (row.hasErrors()) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.addError(row.getRowNumber(), row.getSku(), row.getErrors());
                    continue;
                }

                createOrUpdateProduct(row, tenantId, categoryMap, brandMap, uomMap);
                result.setSuccessCount(result.getSuccessCount() + 1);

            } catch (Exception e) {
                log.error("Error processing row {}: {}", row.getRowNumber(), e.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
                result.addError(row.getRowNumber(), row.getSku(), List.of(e.getMessage()));
            }
        }

        log.info("Import completed: {} success, {} errors, {} skipped out of {} total",
                result.getSuccessCount(), result.getErrorCount(), result.getSkippedCount(), result.getTotalRows());

        return result;
    }

    private void validateRow(ProductImportDto row, Long tenantId,
                            Map<String, Category> categoryMap,
                            Map<String, Brand> brandMap,
                            Map<String, UnitOfMeasure> uomMap) {

        // Required fields
        if (row.getName() == null || row.getName().isEmpty()) {
            row.addError("Name is required");
        }

        if (row.getSellingPrice() == null) {
            row.addError("Selling price is required");
        }

        // Validate UOM
        if (row.getBaseUomCode() != null && !uomMap.containsKey(row.getBaseUomCode().toUpperCase())) {
            row.addError("Invalid UOM code: " + row.getBaseUomCode());
        }

        // Validate category if provided
        if (row.getCategoryName() != null && !categoryMap.containsKey(row.getCategoryName().toLowerCase())) {
            row.addError("Category not found: " + row.getCategoryName());
        }

        // Validate brand if provided
        if (row.getBrandName() != null && !brandMap.containsKey(row.getBrandName().toLowerCase())) {
            row.addError("Brand not found: " + row.getBrandName());
        }

        // Check for duplicate barcode
        if (row.getBarcode() != null && !row.getBarcode().isEmpty()) {
            if (productRepository.existsByBarcodeAndTenantId(row.getBarcode(), tenantId)) {
                row.addError("Barcode already exists: " + row.getBarcode());
            }
        }
    }

    private void createOrUpdateProduct(ProductImportDto row, Long tenantId,
                                       Map<String, Category> categoryMap,
                                       Map<String, Brand> brandMap,
                                       Map<String, UnitOfMeasure> uomMap) {

        // Check if product already exists by SKU
        Optional<Product> existingProduct = row.getSku() != null ?
                productRepository.findBySkuAndTenantId(row.getSku(), tenantId) : Optional.empty();

        Product product;
        if (existingProduct.isPresent()) {
            product = existingProduct.get();
            // Update existing product
            updateProductFromDto(product, row, categoryMap, brandMap, uomMap);
        } else {
            // Create new product
            product = createProductFromDto(row, tenantId, categoryMap, brandMap, uomMap);
        }

        productRepository.save(product);
    }

    private Product createProductFromDto(ProductImportDto row, Long tenantId,
                                         Map<String, Category> categoryMap,
                                         Map<String, Brand> brandMap,
                                         Map<String, UnitOfMeasure> uomMap) {

        // Generate SKU if not provided
        String sku = row.getSku();
        if (sku == null || sku.isEmpty()) {
            sku = skuGeneratorService.generateSkuFromName(row.getName());
        }

        // Get UOM - default to first available if not specified
        UnitOfMeasure uom = row.getBaseUomCode() != null ?
                uomMap.get(row.getBaseUomCode().toUpperCase()) :
                uomMap.values().stream().findFirst().orElse(null);

        if (uom == null) {
            throw new BusinessException("No unit of measure found");
        }

        return Product.builder()
                .tenantId(tenantId)
                .sku(sku)
                .barcode(row.getBarcode())
                .name(row.getName())
                .description(row.getDescription())
                .shortDescription(row.getShortDescription())
                .category(row.getCategoryName() != null ? categoryMap.get(row.getCategoryName().toLowerCase()) : null)
                .brand(row.getBrandName() != null ? brandMap.get(row.getBrandName().toLowerCase()) : null)
                .baseUom(uom)
                .costPrice(row.getCostPrice() != null ? row.getCostPrice() : BigDecimal.ZERO)
                .sellingPrice(row.getSellingPrice())
                .minSellingPrice(row.getMinSellingPrice())
                .wholesalePrice(row.getWholesalePrice())
                .trackInventory(row.getTrackInventory() != null ? row.getTrackInventory() : true)
                .allowNegativeStock(row.getAllowNegativeStock() != null ? row.getAllowNegativeStock() : false)
                .minStockLevel(row.getMinStockLevel() != null ? row.getMinStockLevel() : BigDecimal.ZERO)
                .reorderPoint(row.getReorderPoint() != null ? row.getReorderPoint() : BigDecimal.ZERO)
                .reorderQuantity(row.getReorderQuantity() != null ? row.getReorderQuantity() : BigDecimal.ZERO)
                .maxStockLevel(row.getMaxStockLevel())
                .active(row.getActive() != null ? row.getActive() : true)
                .service(row.getService() != null ? row.getService() : false)
                .sellable(row.getSellable() != null ? row.getSellable() : true)
                .purchasable(row.getPurchasable() != null ? row.getPurchasable() : true)
                .trackBatch(row.getTrackBatch() != null ? row.getTrackBatch() : false)
                .trackSerial(row.getTrackSerial() != null ? row.getTrackSerial() : false)
                .weight(row.getWeight())
                .weightUnit(row.getWeightUnit())
                .manufacturer(row.getManufacturer())
                .manufacturerPartNumber(row.getManufacturerPartNumber())
                .taxCode(row.getTaxCode())
                .tags(row.getTags())
                .build();
    }

    private void updateProductFromDto(Product product, ProductImportDto row,
                                      Map<String, Category> categoryMap,
                                      Map<String, Brand> brandMap,
                                      Map<String, UnitOfMeasure> uomMap) {

        if (row.getBarcode() != null) product.setBarcode(row.getBarcode());
        if (row.getName() != null) product.setName(row.getName());
        if (row.getDescription() != null) product.setDescription(row.getDescription());
        if (row.getShortDescription() != null) product.setShortDescription(row.getShortDescription());
        if (row.getCategoryName() != null) product.setCategory(categoryMap.get(row.getCategoryName().toLowerCase()));
        if (row.getBrandName() != null) product.setBrand(brandMap.get(row.getBrandName().toLowerCase()));
        if (row.getBaseUomCode() != null) product.setBaseUom(uomMap.get(row.getBaseUomCode().toUpperCase()));
        if (row.getCostPrice() != null) product.setCostPrice(row.getCostPrice());
        if (row.getSellingPrice() != null) product.setSellingPrice(row.getSellingPrice());
        if (row.getMinSellingPrice() != null) product.setMinSellingPrice(row.getMinSellingPrice());
        if (row.getWholesalePrice() != null) product.setWholesalePrice(row.getWholesalePrice());
        if (row.getTrackInventory() != null) product.setTrackInventory(row.getTrackInventory());
        if (row.getAllowNegativeStock() != null) product.setAllowNegativeStock(row.getAllowNegativeStock());
        if (row.getMinStockLevel() != null) product.setMinStockLevel(row.getMinStockLevel());
        if (row.getReorderPoint() != null) product.setReorderPoint(row.getReorderPoint());
        if (row.getReorderQuantity() != null) product.setReorderQuantity(row.getReorderQuantity());
        if (row.getMaxStockLevel() != null) product.setMaxStockLevel(row.getMaxStockLevel());
        if (row.getActive() != null) product.setActive(row.getActive());
        if (row.getService() != null) product.setService(row.getService());
        if (row.getSellable() != null) product.setSellable(row.getSellable());
        if (row.getPurchasable() != null) product.setPurchasable(row.getPurchasable());
        if (row.getTrackBatch() != null) product.setTrackBatch(row.getTrackBatch());
        if (row.getTrackSerial() != null) product.setTrackSerial(row.getTrackSerial());
        if (row.getWeight() != null) product.setWeight(row.getWeight());
        if (row.getWeightUnit() != null) product.setWeightUnit(row.getWeightUnit());
        if (row.getManufacturer() != null) product.setManufacturer(row.getManufacturer());
        if (row.getManufacturerPartNumber() != null) product.setManufacturerPartNumber(row.getManufacturerPartNumber());
        if (row.getTaxCode() != null) product.setTaxCode(row.getTaxCode());
        if (row.getTags() != null) product.setTags(row.getTags());
    }

    private Map<String, Category> loadCategories(Long tenantId) {
        Map<String, Category> map = new HashMap<>();
        categoryRepository.findAllByTenantId(tenantId)
                .forEach(c -> map.put(c.getName().toLowerCase(), c));
        return map;
    }

    private Map<String, Brand> loadBrands(Long tenantId) {
        Map<String, Brand> map = new HashMap<>();
        brandRepository.findAllByTenantId(tenantId)
                .forEach(b -> map.put(b.getName().toLowerCase(), b));
        return map;
    }

    private Map<String, UnitOfMeasure> loadUoms(Long tenantId) {
        Map<String, UnitOfMeasure> map = new HashMap<>();
        uomRepository.findAllByTenantId(tenantId)
                .forEach(u -> map.put(u.getCode().toUpperCase(), u));
        return map;
    }
}
