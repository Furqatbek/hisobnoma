package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APInvoiceLineDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceLineRequest;
import com.hisobnoma.platform.finance.entity.APInvoiceLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface APInvoiceLineMapper {

    @Mapping(target = "apInvoiceId", source = "apInvoice.id")
    APInvoiceLineDto toDto(APInvoiceLine apInvoiceLine);

    List<APInvoiceLineDto> toDtoList(List<APInvoiceLine> apInvoiceLines);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apInvoice", ignore = true)
    @Mapping(target = "lineNumber", ignore = true)
    @Mapping(target = "productSku", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "expenseAccountCode", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "orderedQuantity", ignore = true)
    @Mapping(target = "receivedQuantity", ignore = true)
    @Mapping(target = "varianceQuantity", ignore = true)
    @Mapping(target = "varianceAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    APInvoiceLine toEntity(CreateAPInvoiceLineRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apInvoice", ignore = true)
    @Mapping(target = "lineNumber", ignore = true)
    @Mapping(target = "productSku", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "expenseAccountCode", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "orderedQuantity", ignore = true)
    @Mapping(target = "receivedQuantity", ignore = true)
    @Mapping(target = "varianceQuantity", ignore = true)
    @Mapping(target = "varianceAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateAPInvoiceLineRequest request, @MappingTarget APInvoiceLine apInvoiceLine);
}
