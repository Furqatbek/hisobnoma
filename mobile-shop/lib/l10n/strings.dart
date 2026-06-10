/// uz-Cyrl strings for the shop app.
/// Kept as plain constants; a full localization setup can replace this later
/// without touching the screens (they only reference [S]).
class S {
  S._();

  static const appTitle = 'Дўкон';
  static const catalogTitle = 'Каталог';
  static const searchHint = 'Қидириш...';
  static const allCategories = 'Барчаси';
  static const inStock = 'Мавжуд';
  static const outOfStock = 'Тугаган';
  static const emptyCatalog = 'Ҳозирча маҳсулотлар йўқ';
  static const loadError = 'Юклашда хатолик юз берди';
  static const retry = 'Қайта уриниш';
  static const noResults = 'Ҳеч нарса топилмади';
  static const orderByPhone = 'Қўнғироқ қилиш';
  static const orderByTelegram = 'Telegram орқали буюртма';
  static const description = 'Тавсиф';
  static const loadingMore = 'Юкланмоқда...';

  // Cart
  static const cartTitle = 'Сават';
  static const addToCart = 'Саватга қўшиш';
  static const addedToCart = 'Саватга қўшилди';
  static const emptyCart = 'Сават бўш';
  static const total = 'Жами';
  static const checkout = 'Буюртма бериш';
  static const removeItem = 'Ўчириш';

  // Checkout
  static const checkoutTitle = 'Буюртма';
  static const customerName = 'Исмингиз';
  static const phoneNumber = 'Телефон рақам';
  static const region = 'Туман';
  static const village = 'Қишлоқ / маҳалла';
  static const noteOptional = 'Изоҳ (ихтиёрий)';
  static const submitOrder = 'Буюртмани юбориш';
  static const submitting = 'Юборилмоқда...';
  static const fieldRequired = 'Тўлдириш шарт';
  static const invalidPhone = 'Телефон рақам нотўғри';
  static const orderFailed = 'Буюртма юборилмади. Қайта уриниб кўринг.';
  static const tooManyAttempts = 'Жуда кўп уриниш. Бироздан сўнг уриниб кўринг.';

  // Order success / status
  static const orderAccepted = 'Буюртмангиз қабул қилинди!';
  static const yourOrderNumber = 'Буюртма рақамингиз';
  static const orderSuccessHint =
      'Тез орада сиз билан боғланамиз. Ҳолатини текшириш учун буюртма рақамини сақлаб қўйинг.';
  static const backToCatalog = 'Каталогга қайтиш';
  static const checkOrderStatus = 'Буюртма ҳолатини текшириш';
  static const orderStatusTitle = 'Буюртма ҳолати';
  static const orderNumberField = 'Буюртма рақами';
  static const find = 'Излаш';
  static const orderNotFound = 'Буюртма топилмади. Рақам ва телефонни текширинг.';
  static const orderStatus = 'Ҳолат';

  static const deliveryFee = 'Етказиб бериш';
  static const grandTotal = 'Жами тўлов';
  static const discount = 'Чегирма';

  // Auth / account
  static const loginTitle = 'Кириш';
  static const loginHint = 'Телефон рақамингизга SMS код юборамиз';
  static const sendCode = 'Код юбориш';
  static const codeSent = 'Код юборилди';
  static const smsCode = 'SMS код';
  static const confirmCode = 'Тасдиқлаш';
  static const nameOptional = 'Исмингиз (ихтиёрий)';
  static const invalidCode = 'Код нотўғри ёки муддати ўтган';
  static const myOrders = 'Буюртмаларим';
  static const logout = 'Чиқиш';
  static const noOrdersYet = 'Ҳозирча буюртмалар йўқ';
  static const account = 'Профил';
  static const resendCode = 'Қайта юбориш';
  static const changePhone = 'Рақамни ўзгартириш';

  static const Map<String, String> orderStatusNames = {
    'NEW': 'Янги',
    'CONFIRMED': 'Тасдиқланган',
    'DELIVERING': 'Етказилмоқда',
    'COMPLETED': 'Бажарилган',
    'CANCELLED': 'Бекор қилинган',
  };

  static String statusName(String status) =>
      orderStatusNames[status] ?? status;
}
