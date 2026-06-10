import 'package:hisobnoma_shop/api/catalog_api.dart';
import 'package:hisobnoma_shop/models/delivery.dart';
import 'package:hisobnoma_shop/models/order.dart';
import 'package:hisobnoma_shop/models/page_result.dart';
import 'package:hisobnoma_shop/models/public_category.dart';
import 'package:hisobnoma_shop/models/public_product.dart';

class FakeCatalogApi implements CatalogApi {
  List<PublicProduct> products;
  List<PublicCategory> categories;
  List<DeliveryRegion> regions;
  Map<int, List<DeliveryVillage>> villagesByRegion;
  bool failProducts;
  bool failOrder;
  int orderStatusCode;

  FakeCatalogApi({
    this.products = const [],
    this.categories = const [],
    this.regions = const [],
    this.villagesByRegion = const {},
    this.failProducts = false,
    this.failOrder = false,
    this.orderStatusCode = 400,
    this.myOrders = const [],
  });

  int productCalls = 0;
  OrderRequest? lastOrderRequest;

  @override
  Future<PageResult<PublicProduct>> getProducts({
    String? search,
    int? categoryId,
    int page = 0,
    int size = 20,
  }) async {
    productCalls++;
    if (failProducts) throw ApiException('boom');
    var result = products;
    if (search != null && search.trim().isNotEmpty) {
      result = result
          .where((p) =>
              p.name.toLowerCase().contains(search.trim().toLowerCase()))
          .toList();
    }
    if (categoryId != null) {
      result = result.where((p) => p.categoryId == categoryId).toList();
    }
    return PageResult(content: result, number: 0, totalPages: 1, last: true);
  }

  @override
  Future<PublicProduct> getProduct(int id) async {
    return products.firstWhere((p) => p.id == id);
  }

  @override
  Future<List<PublicCategory>> getCategories() async => categories;

  @override
  Future<List<DeliveryRegion>> getRegions() async => regions;

  @override
  Future<List<DeliveryVillage>> getVillages(int regionId) async =>
      villagesByRegion[regionId] ?? const [];

  @override
  Future<PublicOrder> submitOrder(OrderRequest request) async {
    lastOrderRequest = request;
    if (failOrder) {
      throw ApiException('rejected', statusCode: orderStatusCode);
    }
    return PublicOrder(
      orderNumber: 'WO-000042',
      status: 'NEW',
      totalAmount: 0,
      currency: 'UZS',
    );
  }

  @override
  Future<PublicOrder> getOrderStatus(String orderNumber, String phone) async {
    return PublicOrder(
      orderNumber: orderNumber,
      status: 'CONFIRMED',
      totalAmount: 12000,
      currency: 'UZS',
    );
  }

  // ---- auth ----

  String? requestedOtpPhone;
  String validCode = '123456';
  List<PublicOrder> myOrders;

  @override
  Future<void> requestOtp(String phone) async {
    requestedOtpPhone = phone;
  }

  @override
  Future<AuthSession> verifyOtp(String phone, String code, {String? name}) async {
    if (code != validCode) {
      throw ApiException('invalid code', statusCode: 400);
    }
    return AuthSession(
      token: 'fake-token',
      phone: phone.replaceAll(RegExp(r'[^0-9]'), ''),
      name: name,
    );
  }

  @override
  Future<PageResult<PublicOrder>> getMyOrders(String token,
      {int page = 0, int size = 20}) async {
    if (token != 'fake-token') {
      throw ApiException('unauthorized', statusCode: 401);
    }
    return PageResult(content: myOrders, number: 0, totalPages: 1, last: true);
  }
}

PublicProduct product(int id, String name,
    {double price = 1000, bool inStock = true, int? categoryId}) {
  return PublicProduct(
    id: id,
    name: name,
    price: price,
    currency: 'UZS',
    inStock: inStock,
    categoryId: categoryId,
  );
}
