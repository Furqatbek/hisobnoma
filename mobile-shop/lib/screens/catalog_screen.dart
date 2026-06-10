import 'dart:async';

import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../models/public_category.dart';
import '../models/public_product.dart';
import '../widgets/product_card.dart';
import '../widgets/status_views.dart';
import 'product_detail_screen.dart';

class CatalogScreen extends StatefulWidget {
  final CatalogApi api;

  const CatalogScreen({super.key, required this.api});

  @override
  State<CatalogScreen> createState() => _CatalogScreenState();
}

class _CatalogScreenState extends State<CatalogScreen> {
  final _scrollController = ScrollController();
  final _searchController = TextEditingController();
  Timer? _searchDebounce;

  List<PublicProduct> _products = [];
  List<PublicCategory> _categories = [];
  int? _selectedCategoryId;

  bool _loading = true;
  bool _loadingMore = false;
  bool _error = false;
  int _page = 0;
  bool _lastPage = true;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _loadInitial();
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadInitial() async {
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final results = await Future.wait([
        widget.api.getProducts(
          search: _searchController.text,
          categoryId: _selectedCategoryId,
          page: 0,
        ),
        if (_categories.isEmpty) widget.api.getCategories(),
      ]);
      final page = results[0] as dynamic;
      setState(() {
        _products = List<PublicProduct>.from(page.content as List);
        _page = 0;
        _lastPage = page.last as bool;
        if (results.length > 1) {
          _categories = List<PublicCategory>.from(results[1] as List);
        }
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _loading = false;
        _error = true;
      });
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _lastPage || _loading || _error) return;
    setState(() => _loadingMore = true);
    try {
      final page = await widget.api.getProducts(
        search: _searchController.text,
        categoryId: _selectedCategoryId,
        page: _page + 1,
      );
      setState(() {
        _products.addAll(page.content);
        _page = page.number;
        _lastPage = page.last;
        _loadingMore = false;
      });
    } catch (_) {
      // Silent failure for incremental load; user can pull-to-refresh.
      setState(() => _loadingMore = false);
    }
  }

  void _onScroll() {
    if (_scrollController.position.pixels >
        _scrollController.position.maxScrollExtent - 300) {
      _loadMore();
    }
  }

  void _onSearchChanged(String _) {
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 400), _loadInitial);
  }

  void _onCategorySelected(int? categoryId) {
    setState(() => _selectedCategoryId = categoryId);
    _loadInitial();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(S.catalogTitle)),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              decoration: InputDecoration(
                hintText: S.searchHint,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
          if (_categories.isNotEmpty)
            SizedBox(
              height: 48,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                children: [
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: const Text(S.allCategories),
                      selected: _selectedCategoryId == null,
                      onSelected: (_) => _onCategorySelected(null),
                    ),
                  ),
                  for (final category in _categories)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: Text(category.name),
                        selected: _selectedCategoryId == category.id,
                        onSelected: (_) => _onCategorySelected(category.id),
                      ),
                    ),
                ],
              ),
            ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error) {
      return ErrorRetryView(onRetry: _loadInitial);
    }
    if (_products.isEmpty) {
      final searching = _searchController.text.trim().isNotEmpty ||
          _selectedCategoryId != null;
      return RefreshIndicator(
        onRefresh: _loadInitial,
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            child: SizedBox(
              height: constraints.maxHeight,
              child: EmptyView(
                  message: searching ? S.noResults : S.emptyCatalog),
            ),
          ),
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadInitial,
      child: GridView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.all(12),
        physics: const AlwaysScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
          maxCrossAxisExtent: 220,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 0.72,
        ),
        itemCount: _products.length + (_loadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= _products.length) {
            return const Center(child: CircularProgressIndicator());
          }
          final product = _products[index];
          return ProductCard(
            product: product,
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => ProductDetailScreen(
                  api: widget.api,
                  productId: product.id,
                  initialProduct: product,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
