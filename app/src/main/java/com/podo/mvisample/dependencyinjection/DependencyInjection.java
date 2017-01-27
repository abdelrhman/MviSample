/*
 * Copyright 2016 Hannes Dorfmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.podo.mvisample.dependencyinjection;


import com.podo.mvisample.businesslogic.ShoppingCart;
import com.podo.mvisample.businesslogic.feed.GroupedPagedFeedLoader;
import com.podo.mvisample.businesslogic.feed.HomeFeedLoader;
import com.podo.mvisample.businesslogic.feed.PagingFeedLoader;
import com.podo.mvisample.businesslogic.http.ProductBackendApi;
import com.podo.mvisample.businesslogic.http.ProductBackendApiDecorator;
import com.podo.mvisample.businesslogic.interactor.details.DetailsInteractor;
import com.podo.mvisample.businesslogic.interactor.search.SearchInteractor;
import com.podo.mvisample.businesslogic.searchengine.SearchEngine;
import com.podo.mvisample.view.category.CategoryPresenter;
import com.podo.mvisample.view.checkoutbutton.CheckoutButtonPresenter;
import com.podo.mvisample.view.detail.ProductDetailsPresenter;
import com.podo.mvisample.view.home.HomePresenter;
import com.podo.mvisample.view.menu.MainMenuPresenter;
import com.podo.mvisample.view.search.SearchPresenter;
import com.podo.mvisample.view.selectedcounttoolbar.SelectedCountToolbarPresenter;
import com.podo.mvisample.view.shoppingcartlabel.ShoppingCartLabelPresenter;
import com.podo.mvisample.view.shoppingcartoverview.ShoppingCartOverviewItem;
import com.podo.mvisample.view.shoppingcartoverview.ShoppingCartOverviewPresenter;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * This is just a very simple example that creates dependency injection.
 * In a real project you might would like to use dagger.
 *
 * @author Hannes Dorfmann
 */
public class DependencyInjection {

    // Don't do this in your real app
    private final PublishSubject<Boolean> clearSelectionRelay = PublishSubject.create();
    private final PublishSubject<Boolean> deleteSelectionRelay = PublishSubject.create();

    //
    // Some singletons
    //
    private final Retrofit retrofit = new Retrofit.Builder().baseUrl(ProductBackendApi.BASE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create())
            .build();
    private final ProductBackendApi backendApi = retrofit.create(ProductBackendApi.class);
    private final ProductBackendApiDecorator backendApiDecorator =
            new ProductBackendApiDecorator(backendApi);
    private final MainMenuPresenter mainMenuPresenter = new MainMenuPresenter(backendApiDecorator);
    private final ShoppingCart shoppingCart = new ShoppingCart();
    private final ShoppingCartOverviewPresenter shoppingCartPresenter =
            new ShoppingCartOverviewPresenter(shoppingCart, deleteSelectionRelay, clearSelectionRelay);

    private SearchEngine newSearchEngine() {
        return new SearchEngine(backendApiDecorator);
    }

    private SearchInteractor newSearchInteractor() {
        return new SearchInteractor(newSearchEngine());
    }

    PagingFeedLoader newPagingFeedLoader() {
        return new PagingFeedLoader(backendApiDecorator);
    }

    GroupedPagedFeedLoader newGroupedPagedFeedLoader() {
        return new GroupedPagedFeedLoader(newPagingFeedLoader());
    }

    HomeFeedLoader newHomeFeedLoader() {
        return new HomeFeedLoader(newGroupedPagedFeedLoader(), backendApiDecorator);
    }

    public SearchPresenter newSearchPresenter() {
        return new SearchPresenter(newSearchInteractor());
    }

    public HomePresenter newHomePresenter() {
        return new HomePresenter(newHomeFeedLoader());
    }

    /**
     * This is a singleton
     */
    public MainMenuPresenter getMainMenuPresenter() {
        return mainMenuPresenter;
    }

    public CategoryPresenter newCategoryPresenter() {
        return new CategoryPresenter(backendApiDecorator);
    }

    public ProductDetailsPresenter newProductDetailsPresenter() {
        return new ProductDetailsPresenter(new DetailsInteractor(backendApiDecorator, shoppingCart));
    }

    /**
     * This is a singleton
     */
    public ShoppingCartOverviewPresenter getShoppingCartPresenter() {
        return shoppingCartPresenter;
    }

    public ShoppingCartLabelPresenter newShoppingCartLabelPresenter() {
        return new ShoppingCartLabelPresenter(shoppingCart);
    }

    public CheckoutButtonPresenter newCheckoutButtonPresenter() {
        return new CheckoutButtonPresenter(shoppingCart);
    }

    public SelectedCountToolbarPresenter newSelectedCountToolbarPresenter() {

        Observable<Integer> selectedItemCountObservable =
                shoppingCartPresenter.getViewStateObservable().map(items -> {
                    int selected = 0;
                    for (ShoppingCartOverviewItem item : items) {
                        if (item.isSelected()) selected++;
                    }
                    return selected;
                });

        return new SelectedCountToolbarPresenter(selectedItemCountObservable, clearSelectionRelay,
                deleteSelectionRelay);
    }

    public PublishSubject<Boolean> getClearSelectionRelay() {
        return clearSelectionRelay;
    }
}
