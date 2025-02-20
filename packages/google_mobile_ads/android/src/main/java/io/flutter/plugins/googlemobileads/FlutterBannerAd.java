// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.flutter.plugins.googlemobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdView;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.util.Preconditions;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;

/** A wrapper for {@link AdView}. */
class FlutterBannerAd extends FlutterAd implements FlutterAdLoadedListener {

  @NonNull private final AdInstanceManager manager;
  @NonNull private final String adUnitId;
  @NonNull private final FlutterAdSize size;
  @NonNull private final FlutterAdRequest request;
  @NonNull private final BannerAdCreator bannerAdCreator;
  @Nullable private AdView adView;

  /** Constructs the FlutterBannerAd. */
  public FlutterBannerAd(
      int adId,
      @NonNull AdInstanceManager manager,
      @NonNull String adUnitId,
      @NonNull FlutterAdRequest request,
      @NonNull FlutterAdSize size,
      @NonNull BannerAdCreator bannerAdCreator) {
    super(adId);
    Preconditions.checkNotNull(manager);
    Preconditions.checkNotNull(adUnitId);
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(size);
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.request = request;
    this.size = size;
    this.bannerAdCreator = bannerAdCreator;
  }

  @Override
  public void onAdLoaded() {
    if (adView != null) {
      manager.onAdLoaded(adId, adView.getResponseInfo());
    }
  }

  private AdSize getFullAdSize() {
    // Step 2 - Determine the screen width (less decorations) to use for the ad width.
    Display display = manager.activity.getWindowManager().getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);

    float widthPixels = outMetrics.widthPixels;
    float density = outMetrics.density;

    int adWidth = (int) (widthPixels / density);

    // Step 3 - Get adaptive ad size and return for setting on the ad view.
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(manager.activity, adWidth);
  }

  @Override
  void load() {
    adView = bannerAdCreator.createAdView();
    adView.setAdUnitId(adUnitId);
    //adView.setAdSize(size.getAdSize());
    adView.setAdSize(getFullAdSize());
    adView.setOnPaidEventListener(new FlutterPaidEventListener(manager, this));
    adView.setAdListener(new FlutterBannerAdListener(adId, manager, this));
    adView.loadAd(request.asAdRequest());
  }

  @Nullable
  @Override
  public PlatformView getPlatformView() {
    if (adView == null) {
      return null;
    }
    return new FlutterPlatformView(adView);
  }

  @Override
  void dispose() {
    if (adView != null) {
      adView.destroy();
      adView = null;
    }
  }

  @Nullable
  FlutterAdSize getAdSize() {
    if (adView == null || adView.getAdSize() == null) {
      return null;
    }
    return new FlutterAdSize(adView.getAdSize());
  }
}
