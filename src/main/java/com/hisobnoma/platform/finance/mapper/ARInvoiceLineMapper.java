package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARInvoiceLineDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.entity.ARInvoiceLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ARInvoiceLineMapper {

    @Mapping(target = "arInvoiceId", source = "arInvoice.id")
    @Mapping(target = "profitMargin", expression = "java(arInvoiceLine.getProfitMargin())")
    @Mapping(target = "profitMarginPercent", expression = "java(arInvoiceLine.getProfitMarginPercent())")
    ARInvoiceLineDto toDto(ARInvoiceLine arInvoiceLine);

    List<ARInvoiceLineDto> toDtoList(List<ARInvoiceLine> arInvoiceLines);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "arInvoice", ignore = true)
    @Mapping(target = "lineNumber", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ARInvoiceLine toEntity(CreateARInvoiceLineRequest request);
}
