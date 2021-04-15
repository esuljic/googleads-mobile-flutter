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

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

class FlutterInterstitialAd extends FlutterAd.FlutterOverlayAd {
  private static final String TAG = "FlutterInterstitialAd";

  @NonNull private final AdInstanceManager manager;
  @NonNull private final String adUnitId;
  @NonNull private FlutterAdRequest request;
  @Nullable private InterstitialAd ad;
  @NonNull private FlutterAdLoader flutterAdLoader;

  // TODO: (get rid of builder pattern)
  static class Builder {
    @Nullable private AdInstanceManager manager;
    @Nullable private String adUnitId;
    @Nullable private FlutterAdRequest request;

    public Builder setManager(@NonNull AdInstanceManager manager) {
      this.manager = manager;
      return this;
    }

    public Builder setAdUnitId(@NonNull String adUnitId) {
      this.adUnitId = adUnitId;
      return this;
    }

    public Builder setRequest(@NonNull FlutterAdRequest request) {
      this.request = request;
      return this;
    }

    FlutterInterstitialAd build() {
      if (manager == null) {
        throw new IllegalStateException("AdInstanceManager cannot not be null.");
      } else if (adUnitId == null) {
        throw new IllegalStateException("AdUnitId cannot not be null.");
      } else if (request == null) {
        throw new IllegalStateException("AdRequest cannot not be null.");
      }
      return new FlutterInterstitialAd(manager, adUnitId, request, new FlutterAdLoader());
    }
  }

  public FlutterInterstitialAd(
    @NonNull AdInstanceManager manager,
    @NonNull String adUnitId,
    @NonNull FlutterAdRequest request,
    @NonNull FlutterAdLoader flutterAdLoader) {
    this.manager = manager;
    this.adUnitId = adUnitId;
    this.request = request;
    this.flutterAdLoader = flutterAdLoader;
  }

  @Override
  void load() {
    if (manager != null && adUnitId != null && request != null)
    flutterAdLoader.loadInterstitial(
      manager.activity,
      adUnitId,
      request.asAdRequest(),
      new InterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(
          @NonNull InterstitialAd interstitialAd) {
          FlutterInterstitialAd.this.ad = interstitialAd;
          FlutterInterstitialAd.this.manager.onAdLoaded(FlutterInterstitialAd.this);
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          FlutterInterstitialAd.this.manager.onAdFailedToLoad(
            FlutterInterstitialAd.this,
            new FlutterAd.FlutterLoadAdError(loadAdError));
        }
      }
    );
  }

  @Override
  public void show() {
    if (ad == null) {
      Log.e(TAG, "The interstitial wasn't loaded yet.");
      return;
    }
    ad.setFullScreenContentCallback(new FlutterFullScreenContentCallback(manager, this));
    ad.show(manager.activity);
  }
}
