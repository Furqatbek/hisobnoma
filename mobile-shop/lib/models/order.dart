/// Checkout payload sent to POST /api/v1/web/orders.
class OrderRequest {
  final String customerName;
  final String phone;
  final int? regionId;
  final int? villageId;
  final String? note;
  final List<OrderRequestLine> lines;

  const OrderRequest({
    required this.customerName,
    required this.phone,
    this.regionId,
    this.villageId,
    this.note,
    required this.lines,
  });

  Map<String, dynamic> toJson() => {
        'customerName': customerName,
        'phone': phone,
        if (regionId != null) 'regionId': regionId,
        if (villageId != null) 'villageId': villageId,
        if (note != null && note!.isNotEmpty) 'note': note,
        'lines': lines.map((l) => l.toJson()).toList(),
      };
}

class OrderRequestLine {
  final int catalogItemId;
  final double quantity;

  const OrderRequestLine({required this.catalogItemId, required this.quantity});

  Map<String, dynamic> toJson() => {
        'catalogItemId': catalogItemId,
        'quantity': quantity,
      };
}

/// Customer-facing order as returned by checkout and status lookup.
class PublicOrder {
  final String orderNumber;
  final String status;
  final double totalAmount;
  final String currency;
  final List<PublicOrderLine> lines;

  const PublicOrder({
    required this.orderNumber,
    required this.status,
    required this.totalAmount,
    required this.currency,
    this.lines = const [],
  });

  factory PublicOrder.fromJson(Map<String, dynamic> json) {
    return PublicOrder(
      orderNumber: json['orderNumber'] as String? ?? '',
      status: json['status'] as String? ?? '',
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      currency: json['currency'] as String? ?? 'UZS',
      lines: (json['lines'] as List<dynamic>?)
              ?.map((e) => PublicOrderLine.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const [],
    );
  }
}

class PublicOrderLine {
  final String productName;
  final double quantity;
  final double unitPrice;
  final double lineTotal;

  const PublicOrderLine({
    required this.productName,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  factory PublicOrderLine.fromJson(Map<String, dynamic> json) {
    return PublicOrderLine(
      productName: json['productName'] as String? ?? '',
      quantity: (json['quantity'] as num?)?.toDouble() ?? 0,
      unitPrice: (json['unitPrice'] as num?)?.toDouble() ?? 0,
      lineTotal: (json['lineTotal'] as num?)?.toDouble() ?? 0,
    );
  }
}
