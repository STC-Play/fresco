/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.common.callercontext.ContextChain;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.ui.common.OnFadeListener;
import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.PrefetchConfig;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.MountingType;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.utils.MeasureUtils;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/** Simple Fresco Vito component for Litho */
@Nullsafe(Nullsafe.Mode.LOCAL)
@MountSpec(isPureRender = true, canPreallocate = true, poolSize = 15)
public class FrescoVitoImage2Spec {

  public enum Prefetch {
    AUTO,
    YES,
    NO,
    ;

    public static Prefetch parsePrefetch(long value) {
      if (value == 2) {
        return NO;
      }
      if (value == 1) {
        return YES;
      }
      return AUTO;
    }
  }

  @PropDefault static final float imageAspectRatio = 1f;
  @PropDefault static final Prefetch prefetch = Prefetch.AUTO;
  @PropDefault static final boolean mutateDrawables = true;

  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  static FrescoDrawableInterface onCreateMountContent(Context c) {
    return FrescoVitoProvider.getController().createDrawable();
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<AtomicReference<DataSource<Void>>> workingRangePrefetchData) {
    if (FrescoVitoProvider.hasBeenInitialized()
        && FrescoVitoProvider.getConfig().getPrefetchConfig().prefetchWithWorkingRange()) {
      workingRangePrefetchData.set(new AtomicReference<DataSource<Void>>());
    }
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float imageAspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size);
  }

  @OnCalculateCachedValue(name = "imageRequest")
  static VitoImageRequest onCalculateImageRequest(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Uri uri,
      @Prop(optional = true) final @Nullable ImageSource imageSource,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions) {
    ImageSource finalImageSource;
    if (imageSource != null) {
      finalImageSource = imageSource;
    } else {
      finalImageSource = ImageSourceProvider.forUri(uri);
    }
    return FrescoVitoProvider.getImagePipeline()
        .createImageRequest(c.getResources(), finalImageSource, imageOptions);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable Prefetch prefetch,
      @Prop(optional = true) final @Nullable RequestListener prefetchRequestListener,
      @CachedValue VitoImageRequest imageRequest,
      Output<DataSource<Void>> prefetchDataSource) {
    PrefetchConfig config = FrescoVitoProvider.getConfig().getPrefetchConfig();
    if (shouldPrefetchInOnPrepare(prefetch)) {
      prefetchDataSource.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(
                  config.prefetchTargetOnPrepare(),
                  imageRequest,
                  callerContext,
                  prefetchRequestListener,
                  "OnPrepare"));
    }
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      final FrescoDrawableInterface frescoDrawable,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable OnFadeListener onFadeListener,
      @Prop(optional = true) boolean mutateDrawables,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare @Nullable DataSource<Void> prefetchDataSource,
      @FromBoundsDefined Rect viewportDimensions,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData,
      @TreeProp final @Nullable ContextChain contextChain) {
    frescoDrawable.setMutateDrawables(mutateDrawables);
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      return;
    }
    FrescoVitoProvider.getController()
        .fetch(
            frescoDrawable,
            imageRequest,
            callerContext,
            contextChain,
            imageListener,
            onFadeListener,
            viewportDimensions);
    frescoDrawable.getImagePerfListener().onImageMount(frescoDrawable);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
    if (FrescoVitoProvider.getConfig().getPrefetchConfig().cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData);
    }
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      final FrescoDrawableInterface frescoDrawable,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @Prop(optional = true) final @Nullable OnFadeListener onFadeListener,
      @Prop(optional = true) final @Nullable Object callerContext,
      @TreeProp final @Nullable ContextChain contextChain,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare @Nullable DataSource<Void> prefetchDataSource,
      @FromBoundsDefined Rect viewportDimensions,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    // We fetch in both mount and bind in case an unbind event triggered a delayed release.
    // We'll only trigger an actual fetch if needed. Most of the time, this will be a no-op.
    FrescoVitoProvider.getController()
        .fetch(
            frescoDrawable,
            imageRequest,
            callerContext,
            contextChain,
            imageListener,
            onFadeListener,
            viewportDimensions);
    frescoDrawable.getImagePerfListener().onImageBind(frescoDrawable);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
    if (FrescoVitoProvider.getConfig().getPrefetchConfig().cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext c,
      FrescoDrawableInterface frescoDrawable,
      @FromPrepare @Nullable DataSource<Void> prefetchDataSource) {
    frescoDrawable.getImagePerfListener().onImageUnbind(frescoDrawable);
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable);
    } else {
      FrescoVitoProvider.getController().releaseDelayed(frescoDrawable);
    }
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      FrescoDrawableInterface frescoDrawable,
      @FromPrepare @Nullable DataSource<Void> prefetchDataSource) {
    frescoDrawable.getImagePerfListener().onImageUnmount(frescoDrawable);
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      return;
    }
    FrescoVitoProvider.getController().release(frescoDrawable);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop(optional = true) Diff<Uri> uri,
      @Prop(optional = true) Diff<ImageSource> imageSource,
      @Prop(optional = true) Diff<ImageOptions> imageOptions,
      @Prop(optional = true, resType = ResType.FLOAT) Diff<Float> imageAspectRatio,
      @Prop(optional = true) Diff<ImageListener> imageListener) {
    return !ObjectsCompat.equals(uri.getPrevious(), uri.getNext())
        || !ObjectsCompat.equals(imageSource.getPrevious(), imageSource.getNext())
        || !ObjectsCompat.equals(imageOptions.getPrevious(), imageOptions.getNext())
        || !ObjectsCompat.equals(imageAspectRatio.getPrevious(), imageAspectRatio.getNext())
        || !ObjectsCompat.equals(imageListener.getPrevious(), imageListener.getNext());
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(
      ComponentContext c, View host, AccessibilityNodeInfoCompat node) {
    node.setClassName(AccessibilityRole.IMAGE);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, Output<Rect> viewportDimensions) {
    final int width = layout.getWidth();
    final int height = layout.getHeight();
    int paddingX = 0, paddingY = 0;
    if (layout.isPaddingSet()) {
      paddingX = layout.getPaddingLeft() + layout.getPaddingRight();
      paddingY = layout.getPaddingTop() + layout.getPaddingBottom();
    }

    viewportDimensions.set(new Rect(0, 0, width - paddingX, height - paddingY));
  }

  @OnEnteredRange(name = "imagePrefetch")
  static void onEnteredWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Prefetch prefetch,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare @Nullable DataSource<Void> prefetchDataSource,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    if (workingRangePrefetchData == null) {
      return;
    }
    cancelWorkingRangePrefetch(workingRangePrefetchData);
    PrefetchConfig prefetchConfig = FrescoVitoProvider.getConfig().getPrefetchConfig();
    if (shouldPrefetchWithWorkingRange(prefetch)) {
      workingRangePrefetchData.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(
                  prefetchConfig.prefetchTargetWorkingRange(),
                  imageRequest,
                  callerContext,
                  null,
                  "OnEnteredRange"));

      if (prefetchDataSource != null
          && prefetchConfig.cancelOnPreparePrefetchWhenWorkingRangePrefetch()) {
        prefetchDataSource.close();
      }
    }
  }

  @OnExitedRange(name = "imagePrefetch")
  static void onExitedWorkingRange(
      ComponentContext c, @State final AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    cancelWorkingRangePrefetch(workingRangePrefetchData);
  }

  @OnEnteredRange(name = "below3")
  static void onEnteredBelow3WorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(3, callerContext, getUri(imageRequest), "FrescoVitoImage2");
  }

  @OnEnteredRange(name = "below2")
  static void onEnteredBelow2WorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(2, callerContext, getUri(imageRequest), "FrescoVitoImage2");
  }

  @OnEnteredRange(name = "below1")
  static void onEnteredBelowWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(1, callerContext, getUri(imageRequest), "FrescoVitoImage2");
  }

  @OnEnteredRange(name = "visible")
  static void onEnteredVisibleWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(0, callerContext, getUri(imageRequest), "FrescoVitoImage2");
  }

  @OnEnteredRange(name = "above")
  static void onEnteredAboveWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(-1, callerContext, getUri(imageRequest), "FrescoVitoImage2");
  }

  private static @Nullable Uri getUri(VitoImageRequest imageRequest) {
    return imageRequest.finalImageRequest != null
        ? imageRequest.finalImageRequest.getSourceUri()
        : null;
  }

  @OnRegisterRanges
  static void registerWorkingRanges(
      ComponentContext c, @Prop(optional = true) final @Nullable Prefetch prefetch) {
    if (FrescoVitoProvider.hasBeenInitialized()) {
      PrefetchConfig prefetchConfig = FrescoVitoProvider.getConfig().getPrefetchConfig();
      if (shouldPrefetchWithWorkingRange(prefetch)) {
        FrescoVitoImage2.registerImagePrefetchWorkingRange(
            c, new BoundaryWorkingRange(prefetchConfig.prefetchWorkingRangeSize()));
      }

      if (prefetchConfig.prioritizeWithWorkingRange()) {
        FrescoVitoImage2.registerBelow3WorkingRange(
            c, new BelowViewportWorkingRange(3, Integer.MAX_VALUE));
        FrescoVitoImage2.registerBelow2WorkingRange(c, new BelowViewportWorkingRange(2, 2));
        FrescoVitoImage2.registerBelow1WorkingRange(c, new BelowViewportWorkingRange(1, 1));
        FrescoVitoImage2.registerVisibleWorkingRange(c, new InViewportWorkingRange());
        FrescoVitoImage2.registerAboveWorkingRange(c, new AboveViewportWorkingRange());
      }
    }
  }

  static void cancelWorkingRangePrefetch(
      final @Nullable AtomicReference<DataSource<Void>> prefetchData) {
    if (prefetchData == null) {
      return;
    }
    DataSource<Void> dataSource = prefetchData.get();
    if (dataSource != null) {
      dataSource.close();
    }
    prefetchData.set(null);
  }

  static boolean shouldPrefetchInOnPrepare(@Nullable Prefetch prefetch) {
    prefetch = prefetch == null ? Prefetch.AUTO : prefetch;
    switch (prefetch) {
      case YES:
        return true;
      case NO:
        return false;
      default:
        return FrescoVitoProvider.getConfig().getPrefetchConfig().prefetchInOnPrepare();
    }
  }

  static boolean shouldPrefetchWithWorkingRange(@Nullable Prefetch prefetch) {
    prefetch = prefetch == null ? Prefetch.AUTO : prefetch;
    switch (prefetch) {
      case YES:
        return true;
      case NO:
        return false;
      default:
        return FrescoVitoProvider.getConfig().getPrefetchConfig().prefetchWithWorkingRange();
    }
  }
}
