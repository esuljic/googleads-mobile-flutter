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

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.StandardMethodCodec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class GoogleMobileAdsTest {
  private AdInstanceManager testManager;
  private final FlutterAdRequest request = new FlutterAdRequest.Builder().build();
  private static BinaryMessenger mockMessenger;

  private static MethodCall getLastMethodCall() {
    final ArgumentCaptor<ByteBuffer> byteBufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
    verify(mockMessenger)
        .send(
            eq("plugins.flutter.io/google_mobile_ads"),
            byteBufferCaptor.capture(),
            (BinaryMessenger.BinaryReply) isNull());

    return new StandardMethodCodec(new AdMessageCodec())
        .decodeMethodCall((ByteBuffer) byteBufferCaptor.getValue().position(0));
  }

  @Before
  public void setup() {
    mockMessenger = mock(BinaryMessenger.class);
    testManager = new AdInstanceManager(mock(Activity.class), mockMessenger);
  }

  @Test
  public void loadAd() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    assertNotNull(testManager.adForId(0));
    assertEquals(bannerAd, testManager.adForId(0));
    assertEquals(0, testManager.adIdFor(bannerAd).intValue());
  }

  @Test
  public void disposeAd_banner() {
    FlutterBannerAd bannerAd = Mockito.mock(FlutterBannerAd.class);
    testManager.trackAd(bannerAd, 2);
    assertNotNull(testManager.adForId(2));
    assertNotNull(testManager.adIdFor(bannerAd));
    testManager.disposeAd(2);
    verify(bannerAd).destroy();
    assertNull(testManager.adForId(2));
    assertNull(testManager.adIdFor(bannerAd));
  }

  @Test
  public void disposeAd_adManagerBanner() {
    FlutterAdManagerBannerAd adManagerBannerAd = Mockito.mock(FlutterAdManagerBannerAd.class);
    testManager.trackAd(adManagerBannerAd, 2);
    assertNotNull(testManager.adForId(2));
    assertNotNull(testManager.adIdFor(adManagerBannerAd));
    testManager.disposeAd(2);
    verify(adManagerBannerAd).destroy();
    assertNull(testManager.adForId(2));
    assertNull(testManager.adIdFor(adManagerBannerAd));
  }

  @Test
  public void disposeAd_native() {
    FlutterNativeAd flutterNativeAd = Mockito.mock(FlutterNativeAd.class);
    testManager.trackAd(flutterNativeAd, 2);
    assertNotNull(testManager.adForId(2));
    assertNotNull(testManager.adIdFor(flutterNativeAd));
    testManager.disposeAd(2);
    verify(flutterNativeAd).destroy();
    assertNull(testManager.adForId(2));
    assertNull(testManager.adIdFor(flutterNativeAd));
  }

  @Test
  public void adMessageCodec_encodeFlutterAdSize() {
    final AdMessageCodec codec = new AdMessageCodec();
    final ByteBuffer message = codec.encodeMessage(new FlutterAdSize(1, 2));

    assertEquals(codec.decodeMessage((ByteBuffer) message.position(0)), new FlutterAdSize(1, 2));
  }

  @Test
  public void adMessageCodec_encodeFlutterAdRequest() {
    final AdMessageCodec codec = new AdMessageCodec();
    final ByteBuffer message =
        codec.encodeMessage(
            new FlutterAdRequest.Builder()
                .setKeywords(Arrays.asList("1", "2", "3"))
                .setContentUrl("contentUrl")
                .setTestDevices(Arrays.asList("Android", "iOS"))
                .setNonPersonalizedAds(false)
                .build());

    final FlutterAdRequest request =
        (FlutterAdRequest) codec.decodeMessage((ByteBuffer) message.position(0));
    assertEquals(Arrays.asList("1", "2", "3"), request.getKeywords());
    assertEquals("contentUrl", request.getContentUrl());
    assertEquals(Arrays.asList("Android", "iOS"), request.getTestDevices());
    assertEquals(false, request.getNonPersonalizedAds());
  }

  @Test
  public void adMessageCodec_encodeFlutterAdManagerAdRequest() {
    final AdMessageCodec codec = new AdMessageCodec();
    final ByteBuffer message =
        codec.encodeMessage(
            new FlutterAdManagerAdRequest.Builder()
                .setKeywords(Arrays.asList("1", "2", "3"))
                .setContentUrl("contentUrl")
                .setCustomTargeting(Collections.singletonMap("apple", "banana"))
                .setCustomTargetingLists(
                    Collections.singletonMap("cherry", Collections.singletonList("pie")))
                .build());

    assertEquals(
        codec.decodeMessage((ByteBuffer) message.position(0)),
        new FlutterAdManagerAdRequest.Builder()
            .setKeywords(Arrays.asList("1", "2", "3"))
            .setContentUrl("contentUrl")
            .setCustomTargeting(Collections.singletonMap("apple", "banana"))
            .setCustomTargetingLists(
                Collections.singletonMap("cherry", Collections.singletonList("pie")))
            .build());
  }

  @Test
  public void adMessageCodec_encodeFlutterRewardItem() {
    final AdMessageCodec codec = new AdMessageCodec();
    final ByteBuffer message =
        codec.encodeMessage(new FlutterRewardedAd.FlutterRewardItem(23, "coins"));

    assertEquals(
        codec.decodeMessage((ByteBuffer) message.position(0)),
        new FlutterRewardedAd.FlutterRewardItem(23, "coins"));
  }

  @Test
  public void adMessageCodec_encodeFlutterLoadAdError() {
    final AdMessageCodec codec = new AdMessageCodec();
    final ByteBuffer message =
        codec.encodeMessage(new FlutterBannerAd.FlutterLoadAdError(1, "domain", "message"));

    final FlutterAd.FlutterLoadAdError error =
        (FlutterAd.FlutterLoadAdError) codec.decodeMessage((ByteBuffer) message.position(0));
    assertNotNull(error);
    assertEquals(error.code, 1);
    assertEquals(error.domain, "domain");
    assertEquals(error.message, "message");
  }

  @Test
  public void flutterAdListener_onAdLoaded() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onAdLoaded(bannerAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onAdLoaded"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }

  @Test
  public void flutterAdListener_onAdFailedToLoad() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onAdFailedToLoad(bannerAd, new FlutterAd.FlutterLoadAdError(1, "hi", "friend"));

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onAdFailedToLoad"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
    //noinspection rawtypes
    assertThat(
        call.arguments,
        (Matcher) hasEntry("loadAdError", new FlutterAd.FlutterLoadAdError(1, "hi", "friend")));
  }

  @Test
  public void flutterAdListener_onAppEvent() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onAppEvent(bannerAd, "color", "red");

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onAppEvent"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("name", "color"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("data", "red"));
  }

  @Test
  public void flutterAdListener_onApplicationExit() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onApplicationExit(bannerAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onApplicationExit"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }

  @Test
  public void flutterAdListener_onAdOpened() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onAdOpened(bannerAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onAdOpened"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }

  @Test
  public void flutterAdListener_onNativeAdClicked() {
    final FlutterNativeAd nativeAd =
        new FlutterNativeAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setRequest(request)
            .setAdFactory(
                new GoogleMobileAdsPlugin.NativeAdFactory() {
                  @Override
                  public NativeAdView createNativeAd(
                      NativeAd nativeAd, Map<String, Object> customOptions) {
                    return null;
                  }
                })
            .build();
    testManager.trackAd(nativeAd, 0);

    testManager.onNativeAdClicked(nativeAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onNativeAdClicked"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }

  @Test
  public void flutterAdListener_onNativeAdImpression() {
    final FlutterNativeAd nativeAd =
        new FlutterNativeAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setRequest(request)
            .setAdFactory(
                new GoogleMobileAdsPlugin.NativeAdFactory() {
                  @Override
                  public NativeAdView createNativeAd(
                      NativeAd nativeAd, Map<String, Object> customOptions) {
                    return null;
                  }
                })
            .build();
    testManager.trackAd(nativeAd, 0);

    testManager.onNativeAdImpression(nativeAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onNativeAdImpression"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }

  @Test
  public void flutterAdListener_onAdClosed() {
    final FlutterBannerAd bannerAd =
        new FlutterBannerAd.Builder()
            .setManager(testManager)
            .setAdUnitId("testId")
            .setSize(new FlutterAdSize(1, 2))
            .setRequest(request)
            .build();
    testManager.trackAd(bannerAd, 0);

    testManager.onAdClosed(bannerAd);

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onAdClosed"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
  }


  @Test
  public void flutterAdListener_onRewardedAdUserEarnedReward() {
    FlutterAdLoader mockFlutterAdLoader = mock(FlutterAdLoader.class);
    final FlutterRewardedAd ad = new FlutterRewardedAd(
      testManager, "testId", request, null, mockFlutterAdLoader);
    testManager.trackAd(ad, 0);

    testManager.onRewardedAdUserEarnedReward(
      ad, new FlutterRewardedAd.FlutterRewardItem(23, "coins"));

    final MethodCall call = getLastMethodCall();
    assertEquals("onAdEvent", call.method);
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("eventName", "onRewardedAdUserEarnedReward"));
    //noinspection rawtypes
    assertThat(call.arguments, (Matcher) hasEntry("adId", 0));
    //noinspection rawtypes
    assertThat(
      call.arguments,
      (Matcher) hasEntry("rewardItem", new FlutterRewardedAd.FlutterRewardItem(23, "coins")));
  }

  @Test
  public void internalInitDisposesAds() {
    // Set up testManager so that two ads have already been loaded and tracked.
    final FlutterRewardedAd rewarded = Mockito.mock(FlutterRewardedAd.class);
    final FlutterBannerAd banner = Mockito.mock(FlutterBannerAd.class);
    testManager.trackAd(rewarded, 0);
    testManager.trackAd(banner, 1);

    assertEquals(testManager.adIdFor(rewarded), (Integer) 0);
    assertEquals(testManager.adIdFor(banner), (Integer) 1);
    assertEquals(testManager.adForId(0), rewarded);
    assertEquals(testManager.adForId(1), banner);

    // Check that ads are removed and disposed when "_init" is called.
    AdInstanceManager testManagerSpy = Mockito.spy(testManager);
    GoogleMobileAdsPlugin plugin = new GoogleMobileAdsPlugin(null, testManagerSpy);
    Result result = Mockito.mock(Result.class);
    MethodCall methodCall = new MethodCall("_init", null);
    plugin.onMethodCall(methodCall, result);

    verify(testManagerSpy).disposeAllAds();
    verify(result).success(null);
    verify(banner).destroy();
    assertNull(testManager.adForId(0));
    assertNull(testManager.adForId(1));
    assertNull(testManager.adIdFor(rewarded));
    assertNull(testManager.adIdFor(banner));
  }

  @Test(expected = IllegalArgumentException.class)
  public void trackAdThrowsErrorForDuplicateId() {
    final FlutterBannerAd banner = Mockito.mock(FlutterBannerAd.class);
    testManager.trackAd(banner, 0);
    testManager.trackAd(banner, 0);
  }
}
